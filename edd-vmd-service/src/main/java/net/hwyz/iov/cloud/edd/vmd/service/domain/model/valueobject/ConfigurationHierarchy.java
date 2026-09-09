package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * 配置产品树补全视图（CR-047 / US-031）
 * <p>
 * 承载 Configuration 最小投影基础字段 + 沿产品树（Configuration → Variant → Model → CarLine/Platform → Brand）
 * 派生补全的层级字段。上层投影缺失时相应字段为 null（LEFT JOIN 降级，不阻断基础查询）。
 * 非持久化视图，仅用于查询链路组装，不落表。
 * </p>
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Builder
public class ConfigurationHierarchy {

    private Long id;

    /**
     * 配置编码
     */
    private String code;

    /**
     * 配置名称
     */
    private String name;

    /**
     * 本地化名称
     */
    private String nameLocal;

    /**
     * 版本代码（Configuration 唯一直接产品树父引用）
     */
    private String variantCode;

    /**
     * 备注
     */
    private String description;

    /**
     * 派生：车型代码（经 Variant.modelCode）
     */
    private String modelCode;

    /**
     * 派生：车系代码（经 Variant→Model.carLineCode）
     */
    private String carLineCode;

    /**
     * 派生：平台代码（经 Variant→Model.platformCode）
     */
    private String platformCode;

    /**
     * 派生：品牌代码（经 Model.carLineCode→CarLine.brandCode）
     */
    private String brandCode;
}
