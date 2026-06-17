package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 安全常量预置状态值对象
 *
 * @author hwyz_leo
 * @since 2026-06-17
 */
@Getter
@AllArgsConstructor
public enum SecurityConstantState {

    PENDING("PENDING", "待预置"),
    PRESET("PRESET", "已预置"),
    FAILED("FAILED", "预置失败");

    private final String value;
    private final String label;

    public static SecurityConstantState valOf(String val) {
        return Arrays.stream(SecurityConstantState.values())
                .filter(state -> state.value.equals(val))
                .findFirst()
                .orElse(null);
    }
}
