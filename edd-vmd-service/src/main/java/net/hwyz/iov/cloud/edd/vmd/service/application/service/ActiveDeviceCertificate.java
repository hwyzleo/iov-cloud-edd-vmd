package net.hwyz.iov.cloud.edd.vmd.service.application.service;

/**
 * 当前有效设备证书解析结果（VMD-DSN-CR-060 §4.2）。
 * <p>
 * Wrap 收件方的最小证据集合：canonical 序列号 + PKI 申请编号（材料 locator）+ 可选 SHA-256
 * 指纹（作为 framework 交叉校验的 expected evidence）。VMD 不持久化证书 DER/SPKI，材料由 framework
 * 经 {@code pkiRequestId} 从 enrollment result store/PKI 取得。
 *
 * @param certSn              canonical 证书序列号
 * @param pkiRequestId        PKI 申请编号（可为 null：走目录兜底）
 * @param sha256Fingerprint   证书 SHA-256 指纹（可为 null）
 * @author hwyz_leo
 * @since 2026-10-17
 */
public record ActiveDeviceCertificate(String certSn, String pkiRequestId, String sha256Fingerprint) {
}
