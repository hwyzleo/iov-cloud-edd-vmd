package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.domain.DomainObj;

@Slf4j
@Getter
@Setter
@SuperBuilder
public class ConfigurationFeatureCode implements DomainObj<ConfigurationFeatureCode> {

    private Long id;

    /**
     * 配置编码（承接原 buildConfigCode 语义）
     */
    private String configurationCode;

    private String familyCode;

    private String featureCode;

    private String featureType;

    /**
     * 选项族代码(原familyCode, CR-018别名)
     */
    private String optionFamilyCode;

    /**
     * 选项值代码(原featureCode, CR-018别名)
     */
    private String optionCode;

}