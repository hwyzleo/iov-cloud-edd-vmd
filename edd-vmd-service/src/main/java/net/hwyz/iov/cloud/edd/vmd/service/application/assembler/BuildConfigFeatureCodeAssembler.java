package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.BuildConfigFeatureCodeCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BuildConfigFeatureCodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BuildConfigFeatureCode;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface BuildConfigFeatureCodeAssembler {

    BuildConfigFeatureCodeAssembler INSTANCE = Mappers.getMapper(BuildConfigFeatureCodeAssembler.class);

    @Mapping(target = "featureCode", expression = "java(net.hwyz.iov.cloud.framework.common.util.StrUtil.isBlank(buildConfigFeatureCode.getFeatureCode()) ? null : buildConfigFeatureCode.getFeatureCode().split(\",\"))")
    @Mapping(target = "featureName", expression = "java(net.hwyz.iov.cloud.framework.common.util.StrUtil.isBlank(buildConfigFeatureCode.getFeatureName()) ? null : buildConfigFeatureCode.getFeatureName().split(\",\"))")
    BuildConfigFeatureCodeDto fromDomain(BuildConfigFeatureCode buildConfigFeatureCode);

    @Mapping(target = "featureCode", expression = "java(buildConfigFeatureCodeDto.getFeatureCode() == null ? null : String.join(\",\", buildConfigFeatureCodeDto.getFeatureCode()))")
    @Mapping(target = "featureName", expression = "java(buildConfigFeatureCodeDto.getFeatureName() == null ? null : String.join(\",\", buildConfigFeatureCodeDto.getFeatureName()))")
    BuildConfigFeatureCode toDomain(BuildConfigFeatureCodeDto buildConfigFeatureCodeDto);

    @Mapping(target = "featureCode", expression = "java(buildConfigFeatureCodeCmd.getFeatureCode() == null ? null : String.join(\",\", buildConfigFeatureCodeCmd.getFeatureCode()))")
    @Mapping(target = "featureName", expression = "java(buildConfigFeatureCodeCmd.getFeatureName() == null ? null : String.join(\",\", buildConfigFeatureCodeCmd.getFeatureName()))")
    BuildConfigFeatureCode toDomain(BuildConfigFeatureCodeCmd buildConfigFeatureCodeCmd);

    List<BuildConfigFeatureCodeDto> fromDomainList(List<BuildConfigFeatureCode> buildConfigFeatureCodeList);

}