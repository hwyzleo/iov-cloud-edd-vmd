package net.hwyz.iov.cloud.edd.vmd.service.application.replay;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VmdOutbox;

/**
 * 车辆导入补发动作执行结果
 * <p>
 * VMD-DSN-CR-057: 车辆导入补发动作注册表
 * <p>
 * Handler 返回的执行结果：AppService 按 outcome 落逐项动作审计状态，
 * 事件动作携带已构造好的 Outbox 记录，由 AppService 与动作状态同事务提交。
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Getter
@AllArgsConstructor
public class VehicleImportReplayActionResult {

    /**
     * 动作结果类型
     */
    public enum Outcome {
        /** 事件已入Outbox（动作明细状态 QUEUED） */
        QUEUED,
        /** 成功（如生命周期节点补齐） */
        SUCCESS,
        /** 明确跳过（记录跳过原因） */
        SKIPPED,
        /** 可重试失败（瞬态异常） */
        FAILED_RETRYABLE,
        /** 终态失败（确定失败 / 当前事实不存在等） */
        FAILED_FINAL
    }

    private final Outcome outcome;
    private final String skipReason;
    private final String errorCode;
    private final String errorMessage;
    private final String eventId;
    private final VmdOutbox outbox;

    public static VehicleImportReplayActionResult queued(String eventId, VmdOutbox outbox) {
        return new VehicleImportReplayActionResult(Outcome.QUEUED, null, null, null, eventId, outbox);
    }

    public static VehicleImportReplayActionResult success() {
        return new VehicleImportReplayActionResult(Outcome.SUCCESS, null, null, null, null, null);
    }

    public static VehicleImportReplayActionResult skipped(String skipReason) {
        return new VehicleImportReplayActionResult(Outcome.SKIPPED, skipReason, null, null, null, null);
    }

    public static VehicleImportReplayActionResult failedRetryable(String errorCode, String errorMessage) {
        return new VehicleImportReplayActionResult(Outcome.FAILED_RETRYABLE, null, errorCode, errorMessage, null, null);
    }

    public static VehicleImportReplayActionResult failedFinal(String errorCode, String errorMessage) {
        return new VehicleImportReplayActionResult(Outcome.FAILED_FINAL, null, errorCode, errorMessage, null, null);
    }
}
