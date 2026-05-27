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
public class BuildConfig implements DomainObj<BuildConfig> {

    private Long id;

    private String platformCode;

    private String carLineCode;

    private String modelCode;

    private String baseModelCode;

    private String code;

    private String name;

    private String nameEn;

    private String vehicleStageCode;

    private Boolean enable;

    private Integer sort;

}