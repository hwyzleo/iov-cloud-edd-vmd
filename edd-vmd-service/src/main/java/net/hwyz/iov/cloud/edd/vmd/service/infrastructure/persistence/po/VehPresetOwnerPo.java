package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.*;
import lombok.experimental.SuperBuilder;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;

/**
 * <p>
 * 车辆预设车主表 持久化对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2024-09-25
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_veh_preset_owner")
public class VehPresetOwnerPo extends BasePo {

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
     * 车主真实姓名
     */
    @TableField("real_name")
    private String realName;

    /**
     * 手机所属国家或地区
     */
    @TableField("country_region_code")
    private String countryRegionCode;

    /**
     * 手机号
     */
    @TableField("mobile")
    private String mobile;
}
