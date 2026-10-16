package net.hwyz.iov.cloud.edd.vmd.api.vo.keyprov;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Keyprov Service 统一错误响应（VMD-DSN-CR-059 §6）。
 * <p>
 * 仅承载登记后的 806xxx 稳定错误码、是否可重试、建议动作与脱敏展示信息；
 * 不得携带 Wrapped Key、证书、设备身份明文、KMS 引用或内部异常堆栈。
 *
 * @author hwyz_leo
 * @since 2026-10-16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmdKeyProvisioningError {

    /**
     * 登记后的 806xxx 稳定错误码
     */
    private String code;

    /**
     * 脱敏展示信息
     */
    private String message;

    /**
     * 是否可重试（仅依赖类失败为 true）
     */
    private boolean retryable;

    /**
     * 建议动作：RETRY_SAME_REQUEST_ID / QUERY_STATUS_LATER / DO_NOT_RETRY
     */
    private String action;
}
