package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po;

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
 * 配置项表 持久化对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-02-11
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_config_item")
public class ConfigItemPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 配置项大类
     */
    @TableField("family")
    private String family;

    /**
     * 配置项编码
     */
    @TableField("code")
    private String code;

    /**
     * 配置项名称
     */
    @TableField("name")
    private String name;

    /**
     * 配置项类型
     */
    @TableField("type")
    private String type;

    /**
     * 配置项单位
     */
    @TableField("unit")
    private String unit;

    /**
     * 是否车辆能力
     */
    @TableField("capability")
    private Boolean capability;

    /**
     * 端上是否展示
     */
    @TableField("display")
    private Boolean display;

    /**
     * 端上是否缓存
     */
    @TableField("cache")
    private Boolean cache;
}
