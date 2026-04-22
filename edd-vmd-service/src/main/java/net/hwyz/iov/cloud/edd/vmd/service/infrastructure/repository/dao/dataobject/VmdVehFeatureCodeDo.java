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
 * 车辆特征值表 数据对象
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
@TableName("tb_veh_feature_code")
public class VmdVehFeatureCodeDo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 特征族代码
     */
    @TableField("family_code")
    private String familyCode;

    /**
     * 特征值代码
     */
    @TableField("code")
    private String code;

    /**
     * 特征值名称
     */
    @TableField("name")
    private String name;

    /**
     * 特征值英文名称
     */
    @TableField("name_en")
    private String nameEn;

    /**
     * 特征值代表值
     */
    @TableField("val")
    private String val;

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
