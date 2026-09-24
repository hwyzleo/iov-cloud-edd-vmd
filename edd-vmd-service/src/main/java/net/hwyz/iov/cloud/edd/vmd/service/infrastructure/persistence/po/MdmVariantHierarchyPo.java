package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * 版本产品树补全查询结果 PO（CR-048 / RD-048-4）
 * <p>
 * 在 MdmVariantPo 基础上补充沿产品树（Variant → Model → CarLine/Platform）
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
public class MdmVariantHierarchyPo extends MdmVariantPo {

    private static final long serialVersionUID = 1L;

    /**
     * 派生：车系代码（经 tb_mdm_model.car_line_code）
     */
    private String carLineCode;

    /**
     * 派生：平台代码（经 tb_mdm_model.platform_code）
     */
    private String platformCode;

}
