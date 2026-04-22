package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * 车辆配置项表 数据对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-02-12
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_vehicle_config_item")
public class VmdVehicleConfigItemDo extends BasePo {

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
     * 配置版本
     */
    @TableField("version")
    private String version;

    /**
     * 配置项代码
     */
    @TableField("config_item_code")
    private String configItemCode;

    /**
     * 配置项值
     */
    @TableField("config_item_value")
    private String configItemValue;

    /**
     * 源系统值
     */
    @TableField("source_value")
    private String sourceValue;

    /**
     * 源系统
     */
    @TableField("source_system")
    private String sourceSystem;
}
