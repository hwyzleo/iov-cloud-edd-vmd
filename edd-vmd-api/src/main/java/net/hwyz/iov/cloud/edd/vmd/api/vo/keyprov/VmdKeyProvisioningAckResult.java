package net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 官方 Keyprov Service ACK 结果（VMD-DSN-CR-059 §3/§5）。
 * <p>
 * 成功时承载 canonical {@code security.keyprov.v1.KeyProvisioningAckResult} bytes。
 *
 * @author hwyz_leo
 * @since 2026-10-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "payloadBytes")
public class VmdKeyProvisioningAckResult {

    /**
     * 原 requestId（幂等/关联）
     */
    private String requestId;

    /**
     * canonical 下行 payload 类型；成功时为 security.keyprov.v1.KeyProvisioningAckResult
     */
    private String payloadType;

    /**
     * canonical 协议版本（keyprov-v1）
     */
    private String schemaVersion;

    /**
     * canonical 下行 payload bytes
     */
    private byte[] payloadBytes;

    /**
     * 下行 payload 摘要（sha-256 hex）
     */
    private String payloadDigest;

    /**
     * 结果类别
     */
    private VmdKeyProvisioningOutcome outcome;

    /**
     * 失败时的统一错误响应（成功时为 null）
     */
    private VmdKeyProvisioningError error;
}
