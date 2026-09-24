package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * 版本产品树补全视图（CR-048 / RD-048-4）
 * <p>
 * 承载 Variant 最小投影基础字段 + 沿产品树（Variant → Model → CarLine/Platform）
 * 派生补全的层级字段。上层投影缺失时相应字段为 null（LEFT JOIN 降级，不阻断基础查询）。
 * 非持久化视图，仅用于查询链路组装，不落表。
 * </p>
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Builder
public class VariantHierarchy {

    private Long id;

    /**
     * 版本编码
     */
    private String code;

    /**
     * 版本名称
     */
    private String name;

    /**
     * 本地化名称
     */
    private String nameLocal;

    /**
     * 车型代码（Variant 唯一直接产品树父引用）
     */
    private String modelCode;

    /**
     * 备注
     */
    private String description;

    /**
     * 派生：车系代码（经 Model.carLineCode）
     */
    private String carLineCode;

    /**
     * 派生：平台代码（经 Model.platformCode）
     */
    private String platformCode;

}
