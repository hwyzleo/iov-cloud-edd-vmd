package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 零件导入后置处理动作类型
 * <p>
 * VMD-DSN-CR-056: 零件导入后置处理人工重放（US-062）
 * <p>
 * 首期动作：入站跨域事件 / TSP/OTA/IDK 下游联动 / 绑定事实事件 / 器件安全常量补偿。
 * 通过 PartPostProcessActionRegistry 按稳定动作类型发现执行，新增后置能力无需修改主编排。
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Getter
@AllArgsConstructor
public enum PartPostProcessActionType {

    PART_INBOUND_EVENT("PART_INBOUND_EVENT", "零件入站跨域事件"),
    TSP_SYNC("TSP_SYNC", "TSP 下游联动"),
    OTA_SYNC("OTA_SYNC", "OTA 下游联动"),
    IDK_SYNC("IDK_SYNC", "IDK 下游联动"),
    BINDING_FACT_EVENT("BINDING_FACT_EVENT", "绑定事实事件"),
    SECURITY_PRESET("SECURITY_PRESET", "器件安全常量补偿");

    private final String value;
    private final String label;

    public static PartPostProcessActionType valOf(String val) {
        return Arrays.stream(PartPostProcessActionType.values())
                .filter(type -> type.value.equals(val))
                .findFirst()
                .orElse(null);
    }
}
