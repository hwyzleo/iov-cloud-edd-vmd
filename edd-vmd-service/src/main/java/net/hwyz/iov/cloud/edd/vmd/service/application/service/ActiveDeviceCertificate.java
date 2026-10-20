package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import java.util.Arrays;

/**
 * 当前有效设备证书解析结果（VMD-DSN-CR-060 §4.2 / VMD-DSN-CR-063 §10 扩展）。
 * <p>
 * Wrap 收件方的最小证据集合：canonical 序列号 + PKI 申请编号（材料 locator）+ 可选 SHA-256
 * 指纹 + leafCertificateDigest（SHA-256(leaf DER)，CR-063 复验登记）+ 逻辑证书 Profile。
 * VMD 不持久化证书 DER/SPKI，材料由 framework 经 {@code pkiRequestId} 从 enrollment result
 * store/PKI 取得。
 *
 * @param certSn              canonical 证书序列号
 * @param pkiRequestId        PKI 申请编号（可为 null：走目录兜底）
 * @param sha256Fingerprint   证书 SHA-256 指纹（可为 null）
 * @param leafCertificateDigest SHA-256(原始 leaf DER)（可为 null；新 ECDH-ES Keyprov 门禁要求非空）
 * @param certificateProfile  逻辑证书 Profile（新 ECDH-ES Keyprov 门禁要求 == TBOX_DEVICE_ECDH_ES_P256_V1）
 * @author hwyz_leo
 * @since 2026-10-17
 */
public record ActiveDeviceCertificate(String certSn, String pkiRequestId, String sha256Fingerprint,
                                      byte[] leafCertificateDigest, String certificateProfile) {

    /**
     * 兼容既有 3 参构造（CR-060 旧调用/测试）：缺省 digest 与 Profile 为 null
     */
    public ActiveDeviceCertificate(String certSn, String pkiRequestId, String sha256Fingerprint) {
        this(certSn, pkiRequestId, sha256Fingerprint, null, null);
    }

    public ActiveDeviceCertificate {
        leafCertificateDigest = leafCertificateDigest == null ? null : leafCertificateDigest.clone();
    }

    @Override
    public byte[] leafCertificateDigest() {
        return leafCertificateDigest == null ? null : leafCertificateDigest.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ActiveDeviceCertificate other)) {
            return false;
        }
        return java.util.Objects.equals(certSn, other.certSn)
                && java.util.Objects.equals(pkiRequestId, other.pkiRequestId)
                && java.util.Objects.equals(sha256Fingerprint, other.sha256Fingerprint)
                && java.util.Objects.equals(certificateProfile, other.certificateProfile)
                && Arrays.equals(leafCertificateDigest, other.leafCertificateDigest);
    }

    @Override
    public int hashCode() {
        int result = java.util.Objects.hash(certSn, pkiRequestId, sha256Fingerprint, certificateProfile);
        return 31 * result + Arrays.hashCode(leafCertificateDigest);
    }

    @Override
    public String toString() {
        return "ActiveDeviceCertificate{certSn=" + certSn
                + ", sha256FingerprintPresent=" + (sha256Fingerprint != null)
                + ", leafCertificateDigestPresent=" + (leafCertificateDigest != null)
                + ", certificateProfile=" + certificateProfile + "}";
    }
}
