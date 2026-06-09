package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigurationFeatureCode;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehBuildConfigFeatureCodePo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface BuildConfigFeatureCodeConverter {

    BuildConfigFeatureCodeConverter INSTANCE = Mappers.getMapper(BuildConfigFeatureCodeConverter.class);

    @Mapping(target = "configurationCode", source = "buildConfigCode")
    ConfigurationFeatureCode toDomain(VehBuildConfigFeatureCodePo po);

    List<ConfigurationFeatureCode> toDomainList(List<VehBuildConfigFeatureCodePo> poList);

    @Mapping(target = "buildConfigCode", source = "configurationCode")
    VehBuildConfigFeatureCodePo fromDomain(ConfigurationFeatureCode domain);
}