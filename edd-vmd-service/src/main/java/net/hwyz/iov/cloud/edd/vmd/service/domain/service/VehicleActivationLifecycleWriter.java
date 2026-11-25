package net.hwyz.iov.cloud.edd.vmd.service.domain.service;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * 首次激活事实 → 既有车辆生命周期节点专用写入端口（VMD-DSN-CR-079 / D079-01、D079-02、D079-03）
 * <p>
 * 承载节点：{@code tb_veh_lifecycle(vin, node=VEHICLE_ACTIVE)}，语义为「已消费合法激活事实中
 * {@code occurred_at} 最小值」。写入规则固定：
 * <ol>
 *   <li>节点不存在 → 插入目标时间（{@code APPLIED}）</li>
 *   <li>节点已存在且目标时间严格更早 → 仅向前纠正（{@code CORRECTED}）</li>
 *   <li>节点已存在且目标时间更晚或相等 → 不动（{@code DUPLICATE}）</li>
 * </ol>
 * 并发由 {@code uk_vin_node(vin,node)} 唯一键 + 条件更新 + 重读重判共同保证；
 * 唯一键冲突不得被误记为消费失败或新事实（D079-03）。
 * <p>
 * 本端口为纯终端写入：不写 {@code vmd_outbox}、不发二次事件、不回调 TSP（D079-04）。
 *
 * @author hwyz_leo
 * @since 2026-11-25
 */
public interface VehicleActivationLifecycleWriter {

    /**
     * 写入/收敛 {@code VEHICLE_ACTIVE} 生命周期节点
     *
     * @param vin                车架号
     * @param earliestActivatedAt 事实最早激活时刻（UTC，{@code occurredAt} 收敛值）
     * @return 写入结果（含纠正前后时间证据）
     */
    LifecycleWriteOutcome write(String vin, LocalDateTime earliestActivatedAt);

    /**
     * 生命周期写入结果分类（D079-07 指标口径）
     */
    enum LifecycleWriteResult {

        /** 节点不存在，本次插入 */
        APPLIED,

        /** 节点已存在且时间已收敛（更早或相等），本次未改动 */
        DUPLICATE,

        /** 节点已存在且时间更晚，本次仅向前纠正 */
        CORRECTED
    }

    /**
     * 生命周期写入结论
     *
     * @param result         结果分类
     * @param previousReachTime 纠正前触达时间（无既有时间为 null）
     * @param currentReachTime  写入后触达时间
     */
    record LifecycleWriteOutcome(LifecycleWriteResult result,
                                 Instant previousReachTime,
                                 Instant currentReachTime) {

        /**
         * 插入结果（APPLIED）
         *
         * @param currentReachTime 写入后触达时间
         * @return 结论
         */
        public static LifecycleWriteOutcome applied(Instant currentReachTime) {
            return new LifecycleWriteOutcome(LifecycleWriteResult.APPLIED, null, currentReachTime);
        }

        /**
         * 已收敛结果（DUPLICATE）
         *
         * @param existingReachTime 既有触达时间（可能为 null）
         * @return 结论
         */
        public static LifecycleWriteOutcome duplicate(Instant existingReachTime) {
            return new LifecycleWriteOutcome(LifecycleWriteResult.DUPLICATE,
                    existingReachTime, existingReachTime);
        }

        /**
         * 向前纠正结果（CORRECTED）
         *
         * @param previousReachTime 纠正前触达时间（可能为 null）
         * @param currentReachTime  纠正后触达时间
         * @return 结论
         */
        public static LifecycleWriteOutcome corrected(Instant previousReachTime, Instant currentReachTime) {
            return new LifecycleWriteOutcome(LifecycleWriteResult.CORRECTED,
                    previousReachTime, currentReachTime);
        }

        /**
         * 是否为时间向前纠正（需在台账留 {@code LIFECYCLE_CORRECTED} 证据）
         *
         * @return true=已向前纠正
         */
        public boolean corrected() {
            return result == LifecycleWriteResult.CORRECTED;
        }
    }
}
