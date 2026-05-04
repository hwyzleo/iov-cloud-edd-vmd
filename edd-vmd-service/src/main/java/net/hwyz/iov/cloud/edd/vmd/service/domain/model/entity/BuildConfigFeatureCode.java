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
public class BuildConfigFeatureCode implements DomainObj<BuildConfigFeatureCode> {

    private Long id;

    private String buildConfigCode;

    private String familyCode;

    private String familyName;

    private String featureCode;

    private String featureName;

    private String featureType;

}