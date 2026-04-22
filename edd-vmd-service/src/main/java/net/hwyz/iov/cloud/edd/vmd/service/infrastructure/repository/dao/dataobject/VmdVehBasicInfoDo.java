package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;

import lombok.experimental.SuperBuilder;
import lombok.*;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;

/**
 * <p>
 * 车辆基础信息表 数据对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2024-09-24
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_veh_basic_info")
public class VmdVehBasicInfoDo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 车架号
     */
    @TableField("vin")
    private String vin;

    /**
     * 工厂代码
     */
    @TableField("manufacturer_code")
    private String manufacturerCode;

    /**
     * 品牌代码
     */
    @TableField("brand_code")
    private String brandCode;

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
    @TableField("build_config_code")
    private String buildConfigCode;

    /**
     * 车辆下线时间
     */
    @TableField("eol_time")
    private Date eolTime;

    /**
     * 最后一次PDI时间
     */
    @TableField("pdi_time")
    private Date pdiTime;

    /**
     * 订单号
     */
    @TableField("order_num")
    private String orderNum;

    /**
     * 整车基线版本
     */
    @TableField("vehicle_base_version")
    private String vehicleBaseVersion;
}
