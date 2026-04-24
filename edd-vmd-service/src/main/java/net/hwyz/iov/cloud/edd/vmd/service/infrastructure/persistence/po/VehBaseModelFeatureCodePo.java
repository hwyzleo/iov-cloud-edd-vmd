package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * 车辆基础车型特征值关系表 持久化对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-02-08
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_veh_base_model_feature_code")
public class VehBaseModelFeatureCodePo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 基础车型代码
     */
    @TableField("base_model_code")
    private String baseModelCode;

    /**
     * 特征族代码
     */
    @TableField("family_code")
    private String familyCode;

    /**
     * 特征值代码
     */
    @TableField("feature_code")
    private String featureCode;

    /**
     * 特征值类型
     */
    @TableField("feature_type")
    private String featureType;
}
