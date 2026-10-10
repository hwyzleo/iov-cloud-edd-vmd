package net.hwyz.iov.cloud.edd.vmd.service.application.postprocess;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 零件导入后置处理动作执行结果
 * <p>
 * VMD-DSN-CR-056: 零件导入后置处理人工重放（US-062）
 * <p>
 * Handler 返回的执行结果：AppService 按 outcome 落动作明细状态，
 * 事件动作携带已构造好的 Outbox 记录，由 AppService 与动作状态同事务提交。
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Getter
@AllArgsConstructor
public class PartPostProcessActionResult {

    /**
     * 动作结果类型
     */
    public enum Outcome {
        /** 事件已入Outbox（动作明细状态 QUEUED） */
        QUEUED,
        /** 成功 */
        SUCCESS,
        /** 明确跳过（记录跳过原因） */
        SKIPPED,
        /** 可重试失败（超时结果未知 / 下游瞬态） */
        FAILED_RETRYABLE,
        /** 终态失败（确定失败 / 候选不存在等） */
        FAILED_FINAL;

        public static Outcome valOf(String val) {
            return Arrays.stream(Outcome.values())
                    .filter(o -> o.name().equals(val))
                    .findFirst()
                    .orElse(null);
        }
    }

    private final Outcome outcome;
    private final String skipReason;
    private final String errorCode;
    private final String errorMessage;
    private final String eventId;
    private final net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VmdOutbox outbox;

    public static PartPostProcessActionResult queued(String eventId, net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VmdOutbox outbox) {
        return new PartPostProcessActionResult(Outcome.QUEUED, null, null, null, eventId, outbox);
    }

    public static PartPostProcessActionResult success() {
        return new PartPostProcessActionResult(Outcome.SUCCESS, null, null, null, null, null);
    }

    public static PartPostProcessActionResult skipped(String skipReason) {
        return new PartPostProcessActionResult(Outcome.SKIPPED, skipReason, null, null, null, null);
    }

    public static PartPostProcessActionResult failedRetryable(String errorCode, String errorMessage) {
        return new PartPostProcessActionResult(Outcome.FAILED_RETRYABLE, null, errorCode, errorMessage, null, null);
    }

    public static PartPostProcessActionResult failedFinal(String errorCode, String errorMessage) {
        return new PartPostProcessActionResult(Outcome.FAILED_FINAL, null, errorCode, errorMessage, null, null);
    }
}
