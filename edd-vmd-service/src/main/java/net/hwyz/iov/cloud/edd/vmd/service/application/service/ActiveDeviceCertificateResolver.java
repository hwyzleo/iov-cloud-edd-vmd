package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyDeviceCertNotFoundException;
import net.hwyz.iov.cloud.edd.vmd.service.common.util.CertificateSerialUtils;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleCertificate;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleCertificateRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 当前有效设备证书解析器（CR-055 §4，CR-060 §4.2）
 * <p>
 * 业务密钥设备证书公钥 Wrap 的收件方证书：解析设备当前有效（ACTIVE）设备身份证书。
 * 首期设备身份证书 Profile 固定为 TBOX_TSP_CLIENT（对齐 D25 设备证书签发域）。
 * 解析失败 fail-closed，不得退回或使用其它证书。
 * <p>
 * CR-060 §4.2：除 serial 外，同时返回 {@code pkiRequestId}（材料 locator）与 SHA-256 指纹
 * （expected evidence），使调用方可构造含直接证据的 RecipientRef，减少一次目录查询。
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActiveDeviceCertificateResolver {

    /**
     * 设备身份证书 Profile（与 CertificateIdentityValidator legacy 对齐）
     */
    private static final String DEVICE_IDENTITY_PROFILE = "TBOX_TSP_CLIENT";

    /**
     * 新 ECDH-ES Keyprov 收件方逻辑证书 Profile（VMD-DSN-CR-063 §10）
     */
    private static final String ECDH_V1_PROFILE = "TBOX_DEVICE_ECDH_ES_P256_V1";

    private final VehicleCertificateRepository vehicleCertificateRepository;

    /**
     * 解析当前有效设备证书序列号
     *
     * @param identity active 绑定设备身份
     * @return 设备证书 canonical 序列号
     * @throws BusinessKeyDeviceCertNotFoundException 无有效设备证书
     */
    public String resolveActiveCertSn(BoundDeviceIdentity identity) {
        return resolveActive(identity).certSn();
    }

    /**
     * 解析当前有效设备证书（serial + pkiRequestId + fingerprint，CR-060 §4.2）
     *
     * @param identity active 绑定设备身份
     * @return 设备证书证据集合（序列号 canonical 化）
     * @throws BusinessKeyDeviceCertNotFoundException 无有效设备证书
     */
    public ActiveDeviceCertificate resolveActive(BoundDeviceIdentity identity) {
        VehicleCertificate cert = vehicleCertificateRepository
                .selectActiveByDeviceSnAndProfile(identity.deviceSn(), DEVICE_IDENTITY_PROFILE);
        if (cert == null) {
            log.warn("设备[{}]无有效设备证书（profile={}），拒绝业务密钥设备下发", identity.deviceSn(), DEVICE_IDENTITY_PROFILE);
            throw new BusinessKeyDeviceCertNotFoundException(
                    "设备[" + identity.deviceSn() + "]无有效设备证书(profile=" + DEVICE_IDENTITY_PROFILE + ")");
        }
        if (cert.getNotAfter() != null && cert.getNotAfter().isBefore(LocalDateTime.now())) {
            log.warn("设备[{}]设备证书已过期 certSn={}, notAfter={}，拒绝业务密钥设备下发",
                    identity.deviceSn(), cert.getCertSn(), cert.getNotAfter());
            throw new BusinessKeyDeviceCertNotFoundException(
                    "设备[" + identity.deviceSn() + "]设备证书已过期: certSn=" + cert.getCertSn());
        }
        // canonical 化（CR-060 §8.2）：Recipient/expectedSerial 口径统一，禁止 VMD 自定义规则
        String certSn = CertificateSerialUtils.canonical(cert.getCertSn());
        return new ActiveDeviceCertificate(certSn, cert.getPkiRequestId(), cert.getCertificateFingerprint(),
                cert.getLeafCertificateDigest(), cert.getCertificateProfile());
    }

    /**
     * 解析当前有效的新 ECDH-ES 逻辑 v1 设备证书（VMD-DSN-CR-063 §10）
     * <p>
     * 新 ECDH-ES Keyprov 请求的收件方证书：要求 ACTIVE 且 Profile==TBOX_DEVICE_ECDH_ES_P256_V1。
     * 无 v1 证书（sign-only 存量）时 fail-closed 抛出，由调用方映射为确定性拒绝（RD-063-5）。
     *
     * @param identity active 绑定设备身份
     * @return 当前有效 v1 设备证书（含 leaf digest / profile）
     * @throws BusinessKeyDeviceCertNotFoundException 无有效 v1 设备证书
     */
    public ActiveDeviceCertificate resolveActiveEcdhV1(BoundDeviceIdentity identity) {
        VehicleCertificate cert = vehicleCertificateRepository
                .selectActiveByDeviceSnAndProfile(identity.deviceSn(), ECDH_V1_PROFILE);
        if (cert == null) {
            log.warn("设备[{}]无有效 v1 设备证书（profile={}），新 ECDH-ES Keyprov fail-closed",
                    identity.deviceSn(), ECDH_V1_PROFILE);
            throw new BusinessKeyDeviceCertNotFoundException(
                    "设备[" + identity.deviceSn() + "]无有效 v1 设备证书(profile=" + ECDH_V1_PROFILE + ")");
        }
        if (cert.getNotAfter() != null && cert.getNotAfter().isBefore(LocalDateTime.now())) {
            log.warn("设备[{}]v1 设备证书已过期 certSn={}, notAfter={}，拒绝新 ECDH-ES Keyprov",
                    identity.deviceSn(), cert.getCertSn(), cert.getNotAfter());
            throw new BusinessKeyDeviceCertNotFoundException(
                    "设备[" + identity.deviceSn() + "]v1 设备证书已过期: certSn=" + cert.getCertSn());
        }
        String certSn = CertificateSerialUtils.canonical(cert.getCertSn());
        return new ActiveDeviceCertificate(certSn, cert.getPkiRequestId(), cert.getCertificateFingerprint(),
                cert.getLeafCertificateDigest(), cert.getCertificateProfile());
    }

    /**
     * 解析与开通批次冻结证书摘要一致的当前有效设备证书序列号（VMD-DSN-CR-063 §10 冻结一致性）。
     * <p>
     * 兼容新 v1（ECDH-ES）与存量 legacy（TBOX_TSP_CLIENT）批次：优先 v1，其次 legacy，
     * 命中冻结摘要即视为一致；两者均不命中视为证书已变化（fail-closed）。
     *
     * @param identity          active 绑定设备身份
     * @param frozenCertSnDigest 批次冻结的证书序列号摘要（sha256Hex(utf8(certSn))，CR-062）
     * @return canonical 证书序列号
     * @throws BusinessKeyDeviceCertNotFoundException 无匹配冻结摘要的有效设备证书
     */
    public String resolveActiveCertSnForProvisioning(BoundDeviceIdentity identity, String frozenCertSnDigest) {
        for (String profile : List.of(ECDH_V1_PROFILE, DEVICE_IDENTITY_PROFILE)) {
            VehicleCertificate cert = vehicleCertificateRepository
                    .selectActiveByDeviceSnAndProfile(identity.deviceSn(), profile);
            if (cert == null || cert.getCertSn() == null) {
                continue;
            }
            if (cert.getNotAfter() != null && cert.getNotAfter().isBefore(LocalDateTime.now())) {
                continue;
            }
            String certSn = CertificateSerialUtils.canonical(cert.getCertSn());
            if (frozenCertSnDigest == null || frozenCertSnDigest.equals(sha256HexUtf8(certSn))) {
                return certSn;
            }
        }
        log.warn("设备[{}]当前无匹配冻结摘要的有效设备证书，完成前证书复核 fail-closed: deviceSn={}",
                identity.deviceSn(), identity.deviceSn());
        throw new BusinessKeyDeviceCertNotFoundException(
                "设备[" + identity.deviceSn() + "]当前设备证书与开通批次冻结证书不一致或已失效");
    }

    private String sha256HexUtf8(String value) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 摘要计算失败", e);
        }
    }
}
