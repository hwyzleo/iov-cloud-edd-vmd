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
 * 车辆版本选项值关系表 持久化对象（原tb_veh_base_model_feature_code→tb_mdm_variant_option_code，CR-019重命名）
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
@TableName("tb_mdm_variant_option_code")
public class MdmVariantOptionCodePo extends BasePo {

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
     * 选项族代码(原family_code)
     */
    @TableField("option_family_code")
    private String optionFamilyCode;

    /**
     * 选项值代码(原feature_code)
     */
    @TableField("option_code")
    private String optionCode;

    /**
     * 选项值类型(原feature_type)
     */
    @TableField("feature_type")
    private String optionType;
}
