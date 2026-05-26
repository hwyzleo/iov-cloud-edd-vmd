package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 数据来源枚举类
 * MDM: 来自MDM系统
 * MANUAL: 本地手动维护
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum SourceType {

    MDM("MDM", "来自MDM系统"),
    MANUAL("MANUAL", "本地手动维护");

    private final String value;
    private final String label;

    public static SourceType valOf(String val) {
        return Arrays.stream(SourceType.values())
                .filter(sourceType -> sourceType.value.equals(val))
                .findFirst()
                .orElse(null);
    }

}
