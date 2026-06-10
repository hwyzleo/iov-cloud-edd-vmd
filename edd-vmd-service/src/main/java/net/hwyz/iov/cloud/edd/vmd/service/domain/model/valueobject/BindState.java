package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import lombok.AllArgsConstructor;

import java.util.Arrays;

/**
 * 车辆-零件绑定状态枚举类
 * <p>
 * 落在 vehicle_part 表
 *
 * @author hwyz_leo
 */
@AllArgsConstructor
public enum BindState {

    INACTIVE(0, "已解绑"),
    ACTIVE(1, "绑定中");

    public final int value;
    public final String label;

    public static BindState valOf(Integer val) {
        return Arrays.stream(BindState.values())
                .filter(bindState -> bindState.value == val)
                .findFirst()
                .orElse(null);
    }

}
