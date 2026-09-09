package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * 配置产品树补全查询结果 PO（CR-047 / US-031）
 * <p>
 * 在 MdmConfigurationPo 基础上补充沿产品树（Configuration → Variant → Model → CarLine/Platform → Brand）
 * LEFT JOIN 派生补全的层级字段。非持久化视图，仅作 MyBatis JOIN 查询结果载体，不落表。
 * </p>
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MdmConfigurationHierarchyPo extends MdmConfigurationPo {

    private static final long serialVersionUID = 1L;

    /**
     * 派生：车型代码（经 tb_mdm_variant.model_code）
     */
    private String modelCode;

    /**
     * 派生：车系代码（经 tb_mdm_model.car_line_code）
     */
    private String carLineCode;

    /**
     * 派生：平台代码（经 tb_mdm_model.platform_code）
     */
    private String platformCode;

    /**
     * 派生：品牌代码（经 tb_mdm_car_line.brand_code）
     */
    private String brandCode;
}
