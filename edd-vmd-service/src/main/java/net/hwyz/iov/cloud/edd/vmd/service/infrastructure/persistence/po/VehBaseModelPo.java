package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * 车辆基础车型表 持久化对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-01-19
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_veh_base_model")
public class VehBaseModelPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 平台代码
     */
    @TableField("platform_code")
    private String platformCode;

    /**
     * 车系代码
     */
    @TableField("series_code")
    private String seriesCode;

    /**
     * 车型代码
     */
    @TableField("model_code")
    private String modelCode;

    /**
     * 基础车型代码
     */
    @TableField("code")
    private String code;

    /**
     * 基础车型名称
     */
    @TableField("name")
    private String name;

    /**
     * 基础车型英文名称
     */
    @TableField("name_en")
    private String nameEn;

    /**
     * 是否启用
     */
    @TableField("enable")
    private Boolean enable;

    /**
     * 排序
     */
    @TableField("sort")
    private Integer sort;
}
