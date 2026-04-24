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
 * 配置项枚举值表 持久化对象
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
@TableName("tb_config_item_option")
public class ConfigItemOptionPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 配置项编码
     */
    @TableField("config_item_code")
    private String configItemCode;

    /**
     * 枚举值编码
     */
    @TableField("code")
    private String code;

    /**
     * 枚举值名称
     */
    @TableField("name")
    private String name;
}
