package net.hwyz.iov.cloud.edd.vmd.service.application.service;

/**
 * keyprov 已认证会话上下文（CR-058 Q3=B）。
 * <p>
 * 由 VAGW workload identity + EMQX 已认证会话解析后经 canonical relay 传入；
 * {@code hsmUid} 为不可伪造的设备身份，是 VMD 定位 active 绑定的权威来源。
 * 请求 payload 自报身份（如 device_identity_digest）仅作为待校验业务字段。
 *
 * @param source               认证上下文来源
 * @param hsmUid               已认证归一化设备身份（HSM UID / ecu_uid）
 * @param clientId             传输会话标识
 * @param certSerial           设备证书序列号（可空）
 * @param certFingerprint      设备证书指纹（可空）
 * @param connectionGeneration 连接代际
 * @param brokerSessionId      broker 会话标识
 * @param receivedAtMs         接收时间（UTC epoch ms）
 * @author hwyz_leo
 * @since 2026-10-12
 */
public record KeyprovAuthenticatedContext(
        String source,
        String hsmUid,
        String clientId,
        String certSerial,
        String certFingerprint,
        long connectionGeneration,
        String brokerSessionId,
        long receivedAtMs) {

    /**
     * 已认证 HSM UID 是否有效
     */
    public boolean hasIdentity() {
        return hsmUid != null && !hsmUid.isBlank();
    }
}
