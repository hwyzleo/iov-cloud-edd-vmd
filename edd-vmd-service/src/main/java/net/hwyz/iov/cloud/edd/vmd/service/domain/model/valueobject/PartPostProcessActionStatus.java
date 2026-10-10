package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 零件导入后置处理重放动作状态
 * <p>
 * VMD-DSN-CR-056: 零件导入后置处理人工重放（US-062）
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Getter
@AllArgsConstructor
public enum PartPostProcessActionStatus {

    PENDING("PENDING", "待执行"),
    RUNNING("RUNNING", "执行中"),
    QUEUED("QUEUED", "事件已入Outbox"),
    SUCCESS("SUCCESS", "成功"),
    SKIPPED("SKIPPED", "已跳过"),
    FAILED_RETRYABLE("FAILED_RETRYABLE", "可重试失败"),
    FAILED_FINAL("FAILED_FINAL", "终态失败");

    private final String value;
    private final String label;

    public static PartPostProcessActionStatus valOf(String val) {
        return Arrays.stream(PartPostProcessActionStatus.values())
                .filter(status -> status.value.equals(val))
                .findFirst()
                .orElse(null);
    }

    /**
     * 是否为可重试的失败 / 未完成状态（retryFailedOnly 时继续执行）
     */
    public boolean retryable() {
        return this == PENDING || this == RUNNING || this == FAILED_RETRYABLE || this == FAILED_FINAL;
    }
}
