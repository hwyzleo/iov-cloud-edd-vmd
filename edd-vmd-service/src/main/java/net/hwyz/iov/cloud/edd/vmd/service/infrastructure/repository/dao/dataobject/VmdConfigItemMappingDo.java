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
 * 配置项映射表 数据对象
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
@TableName("tb_config_item_mapping")
public class VmdConfigItemMappingDo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 配置项代码
     */
    @TableField("config_item_code")
    private String configItemCode;

    /**
     * 源系统
     */
    @TableField("source_system")
    private String sourceSystem;

    /**
     * 源系统代码
     */
    @TableField("source_code")
    private String sourceCode;

    /**
     * 源系统值
     */
    @TableField("source_value")
    private String sourceValue;

    /**
     * 映射的枚举值编码
     */
    @TableField("target_option_code")
    private String targetOptionCode;

    /**
     * 映射值
     */
    @TableField("target_value")
    private String targetValue;
}
