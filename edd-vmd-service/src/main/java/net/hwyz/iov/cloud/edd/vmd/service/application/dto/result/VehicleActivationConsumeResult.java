package net.hwyz.iov.cloud.edd.vmd.service.application.dto.result;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.ActivationFactConsumeStatus;

/**
 * 首次激活事实消费结论（VMD-DSN-CR-077 §6.2）
 * <p>
 * 供消费者记录指标与结构化日志；状态集合不含 {@code CONFLICT}（R-10）：
 * 摘要差异以 {@code digestMismatch=true} 观测，不改变终态。
 *
 * @param status         台账终态
 * @param applied        是否作用于投影（插入或向前纠正）
 * @param corrected      是否为向前纠正
 * @param digestMismatch 是否摘要不一致（合法重投，仅观测）
 * @param vinUnresolved  VIN 是否未在 VMD 建档（仅事实投影，不建车）
 * @author hwyz_leo
 */
public record VehicleActivationConsumeResult(
        ActivationFactConsumeStatus status,
        boolean applied,
        boolean corrected,
        boolean digestMismatch,
        boolean vinUnresolved) {

    /**
     * 首次记录并作用于投影
     *
     * @param corrected     是否为向前纠正
     * @param vinUnresolved VIN 是否未建档
     * @return 消费结论
     */
    public static VehicleActivationConsumeResult recorded(boolean corrected, boolean vinUnresolved) {
        return new VehicleActivationConsumeResult(
                ActivationFactConsumeStatus.RECORDED, true, corrected, false, vinUnresolved);
    }

    /**
     * 已登记但未作用于投影（晚到事实：投影保持最早）
     *
     * @param vinUnresolved VIN 是否未建档
     * @return 消费结论
     */
    public static VehicleActivationConsumeResult recordedWithoutEffect(boolean vinUnresolved) {
        return new VehicleActivationConsumeResult(
                ActivationFactConsumeStatus.RECORDED, false, false, false, vinUnresolved);
    }

    /**
     * 重复（含摘要不一致的合法重投）
     *
     * @param digestMismatch 是否摘要不一致
     * @return 消费结论
     */
    public static VehicleActivationConsumeResult duplicate(boolean digestMismatch) {
        return new VehicleActivationConsumeResult(
                ActivationFactConsumeStatus.DUPLICATE, false, false, digestMismatch, false);
    }

    /**
     * 隔离（契约错误 / 真冲突）
     *
     * @return 消费结论
     */
    public static VehicleActivationConsumeResult quarantined() {
        return new VehicleActivationConsumeResult(
                ActivationFactConsumeStatus.QUARANTINED, false, false, false, false);
    }

    /**
     * 失败（技术错误重试耗尽）
     *
     * @return 消费结论
     */
    public static VehicleActivationConsumeResult failed() {
        return new VehicleActivationConsumeResult(
                ActivationFactConsumeStatus.FAILED, false, false, false, false);
    }

}
