package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;

import lombok.experimental.SuperBuilder;
import lombok.*;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;

/**
 * <p>
 * 车辆品牌表 持久化对象
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
@TableName("tb_veh_brand")
public class VehBrandPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 品牌代码
     */
    @TableField("code")
    private String code;

    /**
     * 品牌名称
     */
    @TableField("name")
    private String name;

    /**
     * 品牌英文名称
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
