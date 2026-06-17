package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import lombok.AllArgsConstructor;
import lombok.Getter;

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

    private final String code;
    private final String desc;

    public static SecurityConstantState of(String code) {
        for (SecurityConstantState state : values()) {
            if (state.code.equals(code)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown SecurityConstantState code: " + code);
    }
}
