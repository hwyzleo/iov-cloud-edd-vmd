package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.certificateprofile.CertificateProfileRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.certificateprofile.RegisteredCertificateProfile;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateCsrContainsForbiddenValueException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateCsrInvalidException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateCsrSubjectMismatchException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateDeviceCategoryMismatchException;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.security.CsrUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 设备证书身份校验器（CR-054，统一校验内核；VMD-DSN-CR-063 §4.3 扩展）
 * <p>
 * 按 TBOX-SEC-DSN-CR-015 §3 固定顺序执行 CSR 密码学与身份语义校验：
 * PKCS#10 解析 → CSR 自签名（PoP）→ 单值 CN → CN == 绑定 hsm_uid（规范化）→
 * 声明 ecu_uid 三方一致性 → Subject/SAN 禁止 VIN/device_sn → 逻辑 Profile 注册表门禁。
 * 任一步失败 fail-closed 抛 VMD 业务异常，禁止调用方绕过或各自复制判断。
 * <p>
 * CR-063：Profile 白名单改走 {@link CertificateProfileRegistry}；keyprovEligible 的逻辑 Profile
 * （本期 TBOX_DEVICE_ECDH_ES_P256_V1）额外执行设备类别校验与 {@link DeviceCertificateIdentityPolicy}
 * 双用途门禁（EC P-256 / KU / EKU / critical 扩展）。存量 TBOX_TSP_CLIENT 保持 legacy 校验口径不变。
 *
 * @author hwyz_leo
 * @since 2026-09-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CertificateIdentityValidator {

    private final CertificateProfileRegistry certificateProfileRegistry;
    private final DeviceCertificateIdentityPolicy deviceCertificateIdentityPolicy;

    /**
     * 校验 CSR 与绑定设备身份，返回解析后的身份要素
     *
     * @param csrDerBase64      CSR DER Base64
     * @param identity          active 绑定设备身份（含权威 hsm_uid）
     * @param declaredEcuUid    调用方声明 ecu_uid（可为空，非权威）
     * @param certificateProfile 证书 Profile
     * @return 解析后的 CSR 身份要素（CN / SPKI SHA-256 / 指纹 / 公钥 / 用途 / 扩展）
     */
    public ParsedCsr validate(String csrDerBase64, BoundDeviceIdentity identity,
                              String declaredEcuUid, String certificateProfile) {
        // 1. PKCS#10 解析 + 单值 CN（fail-closed）
        String cn;
        try {
            cn = CsrUtils.parseCommonName(csrDerBase64);
        } catch (IllegalArgumentException e) {
            throw new CertificateCsrInvalidException(e.getMessage());
        }

        // 2. CSR 自签名（持有性证明 PoP）
        boolean selfSignatureValid = CsrUtils.verifySignature(csrDerBase64);
        if (!selfSignatureValid) {
            throw new CertificateCsrInvalidException("CSR签名无效");
        }

        // 3. CN == 绑定 hsm_uid（规范化后精确相等，不得截断/补零/模糊匹配）
        String normalizedCn = CsrUtils.normalizeUid(cn);
        if (!identity.hsmUid().equals(normalizedCn)) {
            throw new CertificateCsrSubjectMismatchException(
                    "CSR Subject CN=" + cn + " 与绑定hsm_uid=" + identity.hsmUid() + " 不一致");
        }

        // 4. 声明 ecu_uid 三方一致性（字段存在时必须一致；缺失时不得跳过 UID 门禁）
        if (StrUtil.isNotBlank(declaredEcuUid)
                && !identity.hsmUid().equals(CsrUtils.normalizeUid(declaredEcuUid))) {
            throw new CertificateCsrSubjectMismatchException(
                    "请求ecu_uid=" + declaredEcuUid + " 与绑定hsm_uid=" + identity.hsmUid() + " 不一致");
        }

        // 5. Subject/SAN 不得包含 VIN 或 device_sn（明确的禁止项扫描）
        if (CsrUtils.subjectOrSanContains(csrDerBase64, List.of(identity.vin(), identity.deviceSn()))) {
            throw new CertificateCsrContainsForbiddenValueException(
                    "CSR Subject/SAN 包含 VIN 或 device_sn，禁止签发");
        }

        // 6. 逻辑 Profile 注册表门禁（未知/停用拒绝；不得把外部字符串当物理 Provisioner 名）
        RegisteredCertificateProfile registered = certificateProfileRegistry.requireEnabled(certificateProfile);

        // 7. keyprovEligible Profile：设备类别校验 + 双用途/扩展门禁（CR-063 §4.1/§4.3）
        if (registered.keyprovEligible()) {
            if (identity.deviceCategory() != null
                    && !registered.deviceCategory().equalsIgnoreCase(identity.deviceCategory())) {
                throw new CertificateDeviceCategoryMismatchException(
                        identity.deviceCategory(), registered.deviceCategory());
            }
        }

        // 8. 组装完整 ParsedCsr（公钥/用途/扩展，供幂等、复验与策略判定复用）
        ParsedCsr parsedCsr = new ParsedCsr(
                cn,
                CsrUtils.calculatePublicKeySha256(csrDerBase64),
                CsrUtils.calculateFingerprint(csrDerBase64),
                CsrUtils.extractPublicKey(csrDerBase64),
                CsrUtils.extractKeyUsages(csrDerBase64),
                CsrUtils.extractExtendedKeyUsages(csrDerBase64),
                CsrUtils.extractRequestedExtensions(csrDerBase64),
                selfSignatureValid);

        if (registered.keyprovEligible()) {
            deviceCertificateIdentityPolicy.validateTboxEcdhEsP256V1(parsedCsr);
        }

        return parsedCsr;
    }

}
