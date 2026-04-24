package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;

/**
 * <p>
 * 车辆生产配置表 持久化对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2024-10-11
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_veh_build_config")
public class VehBuildConfigPo extends BasePo {

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
    @TableField("base_model_code")
    private String baseModelCode;

    /**
     * 生产配置代码
     */
    @TableField("code")
    private String code;

    /**
     * 生产配置名称
     */
    @TableField("name")
    private String name;

    /**
     * 车型配置英文名称
     */
    @TableField("name_en")
    private String nameEn;

    /**
     * 车辆阶段代码
     */
    @TableField("vehicle_stage_code")
    private String vehicleStageCode;

    /**
     * 外饰代码
     */
    @TableField("exterior_code")
    private String exteriorCode;

    /**
     * 内饰代码
     */
    @TableField("interior_code")
    private String interiorCode;

    /**
     * 轮毂代码
     */
    @TableField("wheel_code")
    private String wheelCode;
    /**
     * 轮胎代码
     */
    @TableField("tire_code")
    private String tireCode;

    /**
     * 备胎代码
     */
    @TableField("spare_tire_code")
    private String spareTireCode;

    /**
     * 智驾代码
     */
    @TableField("adas_code")
    private String adasCode;

    /**
     * 座椅代码
     */
    @TableField("seat_code")
    private String seatCode;

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
