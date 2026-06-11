package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 零件类型枚举
 * <p>
 * 驱动type-schema校验与下游路由
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum PartType {

    TBOX("TBOX", "车载终端"),
    BTM("BTM", "蓝牙模块"),
    CCP("CCP", "域控制器"),
    IDCM("IDCM", "智能驾驶控制"),
    SIM("SIM", "SIM卡"),
    OTHER("OTHER", "其他");

    private final String value;
    private final String label;

    public static PartType valOf(String val) {
        return Arrays.stream(PartType.values())
                .filter(partType -> partType.value.equals(val))
                .findFirst()
                .orElse(null);
    }
}
