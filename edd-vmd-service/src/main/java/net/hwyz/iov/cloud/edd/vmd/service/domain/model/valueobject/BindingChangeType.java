package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 绑定变更类型枚举
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum BindingChangeType {

    /**
     * 绑定
     */
    BIND("BIND"),

    /**
     * 解绑
     */
    UNBIND("UNBIND"),

    /**
     * 替换
     */
    REPLACE("REPLACE");

    private final String value;

}