package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * 车辆导入事件补发动作类型
 * <p>
 * VMD-DSN-CR-057: 车辆导入补发扩展为按 ImportType 路由的动作补偿
 * <p>
 * 动作按 ImportType 映射（VehicleImportReplayActionRegistry）：
 * <ul>
 *   <li>PRODUCE → PRODUCE_EVENT（CR-039 既有生产事件补发，契约不变）</li>
 *   <li>TOL → TOL_LIFECYCLE_ENSURE / BINDING_EVENT_REPLAY / SOFTWARE_INVENTORY_EVENT_REPLAY</li>
 *   <li>EOL → EOL_LIFECYCLE_ENSURE / BINDING_EVENT_REPLAY / SOFTWARE_INVENTORY_EVENT_REPLAY</li>
 * </ul>
 * 仅注册显式审核过的动作，禁止按类名反射调用任意 Publisher / Subscriber。
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Getter
@AllArgsConstructor
public enum VehicleImportReplayActionType {

    PRODUCE_EVENT("PRODUCE_EVENT", "生产事件补发", List.of("PRODUCE")),
    TOL_LIFECYCLE_ENSURE("TOL_LIFECYCLE_ENSURE", "TOL 生命周期节点补齐", List.of("TOL")),
    EOL_LIFECYCLE_ENSURE("EOL_LIFECYCLE_ENSURE", "EOL 生命周期节点补齐", List.of("EOL")),
    BINDING_EVENT_REPLAY("BINDING_EVENT_REPLAY", "绑定变更事件重放", List.of("TOL", "EOL")),
    SOFTWARE_INVENTORY_EVENT_REPLAY("SOFTWARE_INVENTORY_EVENT_REPLAY", "软件实装事件重放", List.of("TOL", "EOL"));

    private final String value;
    private final String label;
    private final List<String> importTypes;

    public static VehicleImportReplayActionType valOf(String val) {
        return Arrays.stream(VehicleImportReplayActionType.values())
                .filter(type -> type.value.equals(val))
                .findFirst()
                .orElse(null);
    }

    /**
     * 该动作是否适用于指定导入类型
     *
     * @param importType 导入类型
     * @return 是否适用
     */
    public boolean applicableTo(String importType) {
        return importTypes.contains(importType);
    }
}
