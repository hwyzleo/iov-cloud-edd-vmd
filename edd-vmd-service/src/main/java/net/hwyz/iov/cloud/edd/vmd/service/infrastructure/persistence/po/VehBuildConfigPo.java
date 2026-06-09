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

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_veh_build_config")
public class VehBuildConfigPo extends BasePo {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("platform_code")
    private String platformCode;

    @TableField("car_line_code")
    private String carLineCode;

    @TableField("model_code")
    private String modelCode;

    @TableField("base_model_code")
    private String baseModelCode;

    @TableField("code")
    private String code;

    @TableField("name")
    private String name;

    @TableField("name_en")
    private String nameEn;

    @TableField("vehicle_stage_code")
    private String vehicleStageCode;

    @TableField("enable")
    private Boolean enable;

    @TableField("sort")
    private Integer sort;

    @TableField("variant_code")
    private String variantCode;
}