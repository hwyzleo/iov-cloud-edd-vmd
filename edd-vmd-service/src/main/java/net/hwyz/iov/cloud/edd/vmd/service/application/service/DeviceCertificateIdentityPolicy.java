package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateCsrClientAuthMissingException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateCsrEkuConflictException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateCsrKeyUsageInvalidException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateCsrPublicKeyNotAllowedException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateCsrUnsupportedCriticalExtensionException;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.security.CsrExtension;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.security.CsrUtils;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.security.KeyUsageFlag;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * 设备证书身份策略（VMD-DSN-CR-063 §4.3，逻辑 Profile v1 固定规则）。
 * <p>
 * 仅当逻辑 Profile 注册项 {@code keyprovEligible=true}（本期 TBOX_DEVICE_ECDH_ES_P256_V1）时启用：
 * <ul>
 *   <li>Subject DN 非空 / 单 CN / CN==绑定 UID —— 由 {@link CertificateIdentityValidator} 统一校验内核承接；</li>
 *   <li>公钥 == EC P-256（secp256r1），RSA/P-384/P-521 拒绝；</li>
 *   <li>KeyUsage 请求 == digitalSignature + keyAgreement（缺失/冲突/CA 意图拒绝）；</li>
 *   <li>ExtendedKeyUsage 请求 == clientAuth 仅此一项（缺失返回独立 reason，anyEKU/serverAuth/其他过宽拒绝）；</li>
 *   <li>扩展校验采用 OID + criticality + semantic validator allowlist：未知 critical 扩展拒绝，
 *       basicConstraints CA=true（CA 意图）拒绝。</li>
 * </ul>
 * clientAuth 缺失与 EKU 过宽分别返回不同稳定 reason；不得仅凭“已知 OID”放行扩展。
 *
 * @author hwyz_leo
 * @since 2026-10-20
 */
@Slf4j
@Component
public class DeviceCertificateIdentityPolicy {

    /** EKU OID：clientAuth（1.3.6.1.5.5.7.3.2） */
    public static final String OID_EKU_CLIENT_AUTH = "1.3.6.1.5.5.7.3.2";

    /** EKU OID：serverAuth（1.3.6.1.5.5.7.3.1） */
    public static final String OID_EKU_SERVER_AUTH = "1.3.6.1.5.5.7.3.1";

    /** EKU OID：anyExtendedKeyUsage（2.5.29.37.0） */
    public static final String OID_EKU_ANY = "2.5.29.37.0";

    /** basicConstraints OID（2.5.29.19，CA 意图判定） */
    private static final String OID_BASIC_CONSTRAINTS = "2.5.29.19";

    /** 逻辑 Profile v1 已知扩展 allowlist（OID → semantic validator 另行校验，不能仅凭已知放行） */
    private static final Set<String> KNOWN_EXTENSION_OIDS = Set.of(
            "2.5.29.14", // subjectKeyIdentifier
            "2.5.29.15", // keyUsage
            "2.5.29.17", // subjectAltName
            "2.5.29.19", // basicConstraints
            "2.5.29.37"  // extendedKeyUsage
    );

    /** Profile v1 要求的 KeyUsage 位集合（恰好这两项） */
    private static final Set<KeyUsageFlag> REQUIRED_KEY_USAGES =
            Set.of(KeyUsageFlag.DIGITAL_SIGNATURE, KeyUsageFlag.KEY_AGREEMENT);

    /**
     * 校验逻辑 Profile v1（TBOX_DEVICE_ECDH_ES_P256_V1）的 CSR 请求意图。
     * 任一步失败 fail-closed 抛对应稳定 VMD 业务异常。
     *
     * @param csr 完整解析的 CSR 要素（含公钥/用途/扩展）
     */
    public void validateTboxEcdhEsP256V1(ParsedCsr csr) {
        // 1. 公钥必须为 EC P-256（secp256r1）
        if (csr.publicKey() == null || !CsrUtils.isEcP256(csr.publicKey())) {
            throw new CertificateCsrPublicKeyNotAllowedException(
                    "逻辑Profile TBOX_DEVICE_ECDH_ES_P256_V1 仅允许 EC P-256(secp256r1) 公钥");
        }

        // 2. KeyUsage 恰好为 digitalSignature + keyAgreement（缺失任一/多余/CA 意图均拒绝）
        if (!REQUIRED_KEY_USAGES.equals(csr.requestedKeyUsages())) {
            throw new CertificateCsrKeyUsageInvalidException(
                    "逻辑Profile TBOX_DEVICE_ECDH_ES_P256_V1 要求 KeyUsage=digitalSignature+keyAgreement，实际="
                            + csr.requestedKeyUsages());
        }

        // 3. ExtendedKeyUsage：必须含 clientAuth；anyEKU/serverAuth/其他用途视为过宽
        Set<String> eku = csr.requestedExtendedKeyUsages();
        if (!eku.contains(OID_EKU_CLIENT_AUTH)) {
            throw new CertificateCsrClientAuthMissingException(
                    "逻辑Profile TBOX_DEVICE_ECDH_ES_P256_V1 要求 ExtendedKeyUsage 含 clientAuth，实际=" + eku);
        }
        Set<String> extraEku = new HashSet<>(eku);
        extraEku.remove(OID_EKU_CLIENT_AUTH);
        if (!extraEku.isEmpty()) {
            throw new CertificateCsrEkuConflictException(
                    "逻辑Profile TBOX_DEVICE_ECDH_ES_P256_V1 要求 ExtendedKeyUsage 仅含 clientAuth，含过宽用途: " + extraEku);
        }

        // 4. 扩展 allowlist：未知 critical 拒绝；已知 OID 仍需 semantic 校验（basicConstraints CA=true 拒绝）
        for (CsrExtension ext : csr.requestedExtensions()) {
            if (!KNOWN_EXTENSION_OIDS.contains(ext.oid())) {
                if (ext.critical()) {
                    throw new CertificateCsrUnsupportedCriticalExtensionException(
                            "未知 critical 扩展 OID=" + ext.oid());
                }
                log.debug("CSR 非 critical 未知扩展放行（OID={}），语义由签发后复验兜底", ext.oid());
                continue;
            }
            if (OID_BASIC_CONSTRAINTS.equals(ext.oid()) && isCaTrue(ext.value())) {
                throw new CertificateCsrUnsupportedCriticalExtensionException(
                        "basicConstraints CA=true（CA 意图），逻辑Profile v1 拒绝");
            }
        }
    }

    /**
     * 解析 basicConstraints 扩展值判定 CA 意图；解析失败按非 CA 处理（放行，最终由 PKI/复验兜底）
     */
    private boolean isCaTrue(byte[] value) {
        try {
            BasicConstraints bc = BasicConstraints.getInstance(value);
            return bc != null && bc.isCA();
        } catch (Exception e) {
            log.warn("basicConstraints 解析失败，按非 CA 处理: {}", e.getMessage());
            return false;
        }
    }
}
