package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.domain.DomainObj;

/**
 * 配置选项值关系领域对象（原BuildConfigFeatureCode→ConfigurationFeatureCode，CR-018重命名）
 *
 * @author hwyz_leo
 */
@Slf4j
@Getter
@Setter
@SuperBuilder
public class ConfigurationOptionCode implements DomainObj<ConfigurationOptionCode> {

    private Long id;

    /**
     * 配置编码（承接原 buildConfigCode 语义）
     */
    private String configurationCode;

    /**
     * 选项族代码(原familyCode)
     */
    private String optionFamilyCode;

    /**
     * 选项族名称(原familyName)
     */
    private String optionFamilyName;

    /**
     * 选项值代码(原featureCode)
     */
    private String optionCode;

    /**
     * 选项值名称(原featureName)
     */
    private String optionName;

    /**
     * 选项值类型(原featureType)
     */
    private String optionType;

}
