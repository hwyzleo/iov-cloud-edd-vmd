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
 * 车辆特征族表 数据对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-02-06
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_veh_feature_family")
public class VmdVehFeatureFamilyDo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 特征族代码
     */
    @TableField("code")
    private String code;

    /**
     * 特征族名称
     */
    @TableField("name")
    private String name;

    /**
     * 特征族英文名称
     */
    @TableField("name_en")
    private String nameEn;

    /**
     * 特征族分类
     */
    @TableField("type")
    private String type;

    /**
     * 是否强制
     */
    @TableField("mandatory")
    private Boolean mandatory;

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
