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
 * 车辆配置选项值关系表 持久化对象（原VehBuildConfigFeatureCodePo→VehConfigurationFeatureCodePo，CR-018重命名）
 * </p>
 *
 * @author hwyz_leo
 * @since 2024-10-11
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_veh_build_config_feature_code")
public class VehConfigurationOptionCodePo extends BasePo {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("configuration_code")
    private String configurationCode;

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
