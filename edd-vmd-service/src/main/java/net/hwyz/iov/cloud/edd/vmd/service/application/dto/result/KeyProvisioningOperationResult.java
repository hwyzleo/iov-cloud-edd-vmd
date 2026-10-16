package net.hwyz.iov.cloud.edd.vmd.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov.VmdKeyProvisioningOutcome;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VmdErrorCode;

/**
 * Keyprov Service 应用层统一处理结果（VMD-DSN-CR-059）。
 * <p>
 * 由 {@code KeyProvisioningApplicationFacade} 返回，Adapter 映射为各端点 wire DTO。
 * {@code downlinkPayload} 为敏感材料（可能含 Wrapped Key），禁止 toString/日志/持久化。
 *
 * @author hwyz_leo
 * @since 2026-10-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "downlinkPayload")
public class KeyProvisioningOperationResult {

    /**
     * 原 requestId（幂等/关联）
     */
    private String requestId;

    /**
     * canonical 下行 payload 类型（失败时为空）
     */
    private String downlinkPayloadType;

    /**
     * canonical 下行 payload bytes（敏感，禁日志）
     */
    private byte[] downlinkPayload;

    /**
     * 下行 payload 摘要（sha-256 hex，失败时为空）
     */
    private String downlinkPayloadDigest;

    /**
     * Wrapped Key bundle 可接受期限（UTC epoch ms，可空）
     */
    private Long expireAtMs;

    /**
     * 结果类别
     */
    private VmdKeyProvisioningOutcome outcome;

    /**
     * 稳定 806xxx 错误码（成功时为 null）
     */
    private VmdErrorCode errorCode;

    /**
     * 脱敏错误信息（成功时为 null）
     */
    private String errorMessage;

    /**
     * 是否可重试
     */
    private boolean retryable;

    /**
     * 建议动作：RETRY_SAME_REQUEST_ID / QUERY_STATUS_LATER / DO_NOT_RETRY
     */
    private String action;

    /**
     * 是否成功
     */
    public boolean isSuccess() {
        return outcome == VmdKeyProvisioningOutcome.SUCCESS;
    }
}
