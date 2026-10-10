package net.hwyz.iov.cloud.edd.vmd.service.application.replay;

/**
 * 车辆导入补发动作规划目标
 * <p>
 * VMD-DSN-CR-057: 车辆导入补发动作注册表
 * <p>
 * 单个目标对应逐项审计唯一键 (replay_id, action_type, aggregate_type, aggregate_id, aggregate_version)：
 * <ul>
 *   <li>生产事件：VEHICLE / VIN / 快照版本</li>
 *   <li>生命周期补齐：VEHICLE_LIFECYCLE / VIN / 0</li>
 *   <li>绑定事件重放：VEHICLE_PART / 绑定ID / 事件序 seq</li>
 *   <li>软件实装事件重放：PART_SOFTWARE_INSTALLATION / 零件ID / 软件清单版本</li>
 * </ul>
 *
 * @param aggregateType    聚合类型（VehicleImportReplayAggregateType.value）
 * @param aggregateId      聚合ID
 * @param aggregateVersion 聚合版本
 * @param sourceRecordId   原批次候选记录标识（如 partCode:sn，可空）
 * @author hwyz_leo
 * @since 2026-10-10
 */
public record VehicleImportReplayActionTarget(String aggregateType, String aggregateId,
                                              Long aggregateVersion, String sourceRecordId) {
}
