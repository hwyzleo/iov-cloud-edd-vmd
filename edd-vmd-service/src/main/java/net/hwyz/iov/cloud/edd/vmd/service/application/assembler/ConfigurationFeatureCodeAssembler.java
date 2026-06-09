package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ConfigurationFeatureCodeCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigurationFeatureCodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigurationFeatureCode;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface ConfigurationFeatureCodeAssembler {

    ConfigurationFeatureCodeAssembler INSTANCE = Mappers.getMapper(ConfigurationFeatureCodeAssembler.class);

    @Mapping(target = "configurationCode", source = "configurationCode")
    @Mapping(target = "featureCode", expression = "java(net.hwyz.iov.cloud.framework.common.util.StrUtil.isBlank(configurationFeatureCode.getFeatureCode()) ? null : configurationFeatureCode.getFeatureCode().split(\",\"))")
    @Mapping(target = "featureName", ignore = true)
    @Mapping(target = "familyName", ignore = true)
    @Mapping(target = "description", ignore = true)
    ConfigurationFeatureCodeDto fromDomain(ConfigurationFeatureCode configurationFeatureCode);

    @Mapping(target = "configurationCode", source = "configurationCode")
    @Mapping(target = "featureCode", expression = "java(configurationFeatureCodeDto.getFeatureCode() == null ? null : String.join(\",\", configurationFeatureCodeDto.getFeatureCode()))")
    ConfigurationFeatureCode toDomain(ConfigurationFeatureCodeDto configurationFeatureCodeDto);

    @Mapping(target = "configurationCode", source = "configurationCode")
    @Mapping(target = "featureCode", expression = "java(configurationFeatureCodeCmd.getFeatureCode() == null ? null : String.join(\",\", configurationFeatureCodeCmd.getFeatureCode()))")
    ConfigurationFeatureCode toDomain(ConfigurationFeatureCodeCmd configurationFeatureCodeCmd);

    List<ConfigurationFeatureCodeDto> fromDomainList(List<ConfigurationFeatureCode> configurationFeatureCodeList);

}
