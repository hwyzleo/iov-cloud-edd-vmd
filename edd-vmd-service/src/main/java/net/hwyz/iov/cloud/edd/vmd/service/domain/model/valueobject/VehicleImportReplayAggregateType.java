package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 车辆导入事件补发逐项动作聚合类型
 * <p>
 * VMD-DSN-CR-057: 车辆导入补发逐项动作审计
 * <p>
 * 逐项审计唯一键 (replay_id, action_type, aggregate_type, aggregate_id, aggregate_version)
 * 中聚合维度的取值：
 * <ul>
 *   <li>VEHICLE：生产事件（aggregateId=VIN，version=快照版本）</li>
 *   <li>VEHICLE_LIFECYCLE：生命周期节点补齐（aggregateId=VIN，version=0）</li>
 *   <li>VEHICLE_PART：绑定事件重放（aggregateId=绑定ID，version=事件序 seq）</li>
 *   <li>PART_SOFTWARE_INSTALLATION：软件实装事件重放（aggregateId=零件ID，version=软件清单版本）</li>
 * </ul>
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Getter
@AllArgsConstructor
public enum VehicleImportReplayAggregateType {

    VEHICLE("VEHICLE", "车辆"),
    VEHICLE_LIFECYCLE("VEHICLE_LIFECYCLE", "车辆生命周期节点"),
    VEHICLE_PART("VEHICLE_PART", "车辆-零件绑定"),
    PART_SOFTWARE_INSTALLATION("PART_SOFTWARE_INSTALLATION", "零件软件实装");

    private final String value;
    private final String label;

    public static VehicleImportReplayAggregateType valOf(String val) {
        return Arrays.stream(VehicleImportReplayAggregateType.values())
                .filter(type -> type.value.equals(val))
                .findFirst()
                .orElse(null);
    }
}
