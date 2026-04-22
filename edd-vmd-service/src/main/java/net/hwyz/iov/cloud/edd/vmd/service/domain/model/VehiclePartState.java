package net.hwyz.iov.cloud.edd.vmd.service.domain.model;

import lombok.AllArgsConstructor;

import java.util.Arrays;

/**
 * 车辆零件状态枚举类
 *
 * @author hwyz_leo
 */
@AllArgsConstructor
public enum VehiclePartState {

    UNBOUND(0, "待绑定"),
    IN_USE(1, "在用"),
    PENDING_REPLACEMENT(2, "待更换"),
    RETIRED(3, "已报废");

    public final int value;
    public final String label;

    public static VehiclePartState valOf(Integer val) {
        return Arrays.stream(VehiclePartState.values())
                .filter(vehiclePartState -> vehiclePartState.value == val)
                .findFirst()
                .orElse(null);
    }

}
