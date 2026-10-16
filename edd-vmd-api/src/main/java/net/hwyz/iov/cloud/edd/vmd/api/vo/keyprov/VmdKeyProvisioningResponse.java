package net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 官方 Keyprov Service 开通申请响应（VMD-DSN-CR-059 §3/§5）。
 * <p>
 * 成功时承载 canonical {@code security.keyprov.v1.KeyProvisioningResponse} bytes；
 * {@code payloadBytes} 含 Wrapped Key，属敏感材料，仅在单次调用栈短暂存在，
 * 禁止 toString/日志/持久化。
 *
 * @author hwyz_leo
 * @since 2026-10-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "payloadBytes")
public class VmdKeyProvisioningResponse {

    /**
     * 原 requestId（幂等/关联）
     */
    private String requestId;

    /**
     * canonical 下行 payload 类型；成功时为 security.keyprov.v1.KeyProvisioningResponse
     */
    private String payloadType;

    /**
     * canonical 协议版本（keyprov-v1）
     */
    private String schemaVersion;

    /**
     * canonical 下行 payload bytes（敏感，禁日志）
     */
    private byte[] payloadBytes;

    /**
     * 下行 payload 摘要（sha-256 hex）
     */
    private String payloadDigest;

    /**
     * Wrapped Key bundle 可接受期限（UTC epoch ms，成功时可空）
     */
    private Long expireAtMs;

    /**
     * 结果类别
     */
    private VmdKeyProvisioningOutcome outcome;

    /**
     * 失败时的统一错误响应（成功时为 null）
     */
    private VmdKeyProvisioningError error;
}
