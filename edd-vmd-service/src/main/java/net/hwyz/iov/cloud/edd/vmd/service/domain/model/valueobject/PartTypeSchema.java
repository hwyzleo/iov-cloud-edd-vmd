package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 零件类型模式值对象
 * <p>
 * 描述零件类型的配置模式，包括是否需要安全常量预置等属性
 *
 * @author hwyz_leo
 * @since 2026-06-24
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartTypeSchema {

    /**
     * 零件类型编码
     */
    private String partType;

    /**
     * HSM唯一标识字段名
     */
    private String hsmUid;

    /**
     * 是否需要安全常量预置
     */
    private boolean needsSecurityConstantPreset;

    /**
     * 描述
     */
    private String description;
}
