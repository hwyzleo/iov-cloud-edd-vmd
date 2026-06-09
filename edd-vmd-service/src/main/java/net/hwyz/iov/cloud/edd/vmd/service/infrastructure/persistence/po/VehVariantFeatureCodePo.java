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
 * 车辆版本特征值关系表 持久化对象（原VehBaseModelFeatureCodePo，CR-016重命名）
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
public class VehVariantFeatureCodePo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 版本代码
     */
    @TableField("variant_code")
    private String variantCode;

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

    /**
     * 选项族代码(原familyCode, CR-018别名)
     */
    @TableField("option_family_code")
    private String optionFamilyCode;

    /**
     * 选项值代码(原featureCode, CR-018别名)
     */
    @TableField("option_code")
    private String optionCode;
}