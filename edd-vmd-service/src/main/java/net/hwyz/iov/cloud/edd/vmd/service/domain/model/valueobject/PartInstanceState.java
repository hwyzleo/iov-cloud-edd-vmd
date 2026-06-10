package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import lombok.AllArgsConstructor;

import java.util.Arrays;

/**
 * 物理零件实例状态枚举类
 * <p>
 * 落在 part_info 表
 *
 * @author hwyz_leo
 */
@AllArgsConstructor
public enum PartInstanceState {

    IN_STOCK(0, "在库"),
    IN_USE(1, "在用"),
    PENDING_REPLACEMENT(2, "待更换"),
    RETIRED(3, "已报废");

    public final int value;
    public final String label;

    public static PartInstanceState valOf(Integer val) {
        return Arrays.stream(PartInstanceState.values())
                .filter(partInstanceState -> partInstanceState.value == val)
                .findFirst()
                .orElse(null);
    }

}
