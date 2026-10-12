package net.hwyz.iov.cloud.edd.vmd.api.vo.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * keyprov canonical relay 请求（CR-058 / EDD-VAGW-DSN-CR-011 §3.5）。
 * <p>
 * VAGW 在通过 EMQX 已认证上下文与 canonical 协议校验后，将 authenticated context、
 * canonical Envelope bytes 与 canonical payload bytes 转发 VMD。
 * {@code canonicalPayload} 为敏感密文，调用方不得对其 toString/日志化。
 * <p>
 * 身份来源：{@code authenticatedHsmUid} 来自 VAGW workload identity + EMQX 已认证会话，
 * 不接受 payload 自报身份；payload 内的 device_identity_digest 仅作为待校验业务字段。
 *
 * @author hwyz_leo
 * @since 2026-10-12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeyprovRelayRequest {

    /**
     * 认证上下文来源（当前 ACL_SESSION_CORRELATED）
     */
    private String source;

    /**
     * 已认证设备身份（归一化 HSM UID / ecu_uid），不可伪造
     */
    private String authenticatedHsmUid;

    /**
     * 传输会话标识（非身份来源）
     */
    private String clientId;

    /**
     * 设备证书序列号（认证期透传，可能为空）
     */
    private String certSerial;

    /**
     * 设备证书指纹（可能为空）
     */
    private String certFingerprint;

    /**
     * 连接代际（重连/session 替换递增）
     */
    private long connectionGeneration;

    /**
     * broker 侧会话标识
     */
    private String brokerSessionId;

    /**
     * 接收时间（UTC epoch ms）
     */
    private long receivedAtMs;

    /**
     * canonical vehicle.common.v1.VehicleMessageEnvelope bytes
     */
    private byte[] envelope;

    /**
     * canonical security.keyprov.v1 payload bytes（敏感密文，禁日志）
     */
    private byte[] canonicalPayload;
}
