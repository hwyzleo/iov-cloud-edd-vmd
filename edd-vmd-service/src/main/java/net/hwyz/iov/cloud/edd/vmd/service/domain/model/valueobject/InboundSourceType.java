package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 零件实例入站来源系统枚举
 * <p>
 * 落在 part_info.source 字段
 * 区别于字典投影表的 SourceType（MDM/MANUAL）
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum InboundSourceType {

    MES("MES", "制造执行系统"),
    MANUAL("MANUAL", "手动导入"),
    WMS("WMS", "仓储管理系统"),
    IQC("IQC", "来料检验"),
    OTHER("OTHER", "其他");

    private final String value;
    private final String label;

    public static InboundSourceType valOf(String val) {
        return Arrays.stream(InboundSourceType.values())
                .filter(sourceType -> sourceType.value.equals(val))
                .findFirst()
                .orElse(null);
    }
}
