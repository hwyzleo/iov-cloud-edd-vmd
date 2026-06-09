package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.BuildConfigFeatureCodeCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BuildConfigFeatureCodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigurationFeatureCode;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface BuildConfigFeatureCodeAssembler {

    BuildConfigFeatureCodeAssembler INSTANCE = Mappers.getMapper(BuildConfigFeatureCodeAssembler.class);

    @Mapping(target = "buildConfigCode", source = "configurationCode")
    @Mapping(target = "featureCode", expression = "java(net.hwyz.iov.cloud.framework.common.util.StrUtil.isBlank(configurationFeatureCode.getFeatureCode()) ? null : configurationFeatureCode.getFeatureCode().split(\",\"))")
    @Mapping(target = "featureName", ignore = true)
    @Mapping(target = "familyName", ignore = true)
    @Mapping(target = "description", ignore = true)
    BuildConfigFeatureCodeDto fromDomain(ConfigurationFeatureCode configurationFeatureCode);

    @Mapping(target = "configurationCode", source = "buildConfigCode")
    @Mapping(target = "featureCode", expression = "java(buildConfigFeatureCodeDto.getFeatureCode() == null ? null : String.join(\",\", buildConfigFeatureCodeDto.getFeatureCode()))")
    ConfigurationFeatureCode toDomain(BuildConfigFeatureCodeDto buildConfigFeatureCodeDto);

    @Mapping(target = "configurationCode", source = "buildConfigCode")
    @Mapping(target = "featureCode", expression = "java(buildConfigFeatureCodeCmd.getFeatureCode() == null ? null : String.join(\",\", buildConfigFeatureCodeCmd.getFeatureCode()))")
    ConfigurationFeatureCode toDomain(BuildConfigFeatureCodeCmd buildConfigFeatureCodeCmd);

    List<BuildConfigFeatureCodeDto> fromDomainList(List<ConfigurationFeatureCode> configurationFeatureCodeList);

}