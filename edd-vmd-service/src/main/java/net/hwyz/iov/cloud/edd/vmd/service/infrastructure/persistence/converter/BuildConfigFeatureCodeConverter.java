package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BuildConfigFeatureCode;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehBuildConfigFeatureCodePo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface BuildConfigFeatureCodeConverter {

    BuildConfigFeatureCodeConverter INSTANCE = Mappers.getMapper(BuildConfigFeatureCodeConverter.class);

    @Mapping(target = "familyName", ignore = true)
    @Mapping(target = "featureName", ignore = true)
    BuildConfigFeatureCode toDomain(VehBuildConfigFeatureCodePo po);

    List<BuildConfigFeatureCode> toDomainList(List<VehBuildConfigFeatureCodePo> poList);

    VehBuildConfigFeatureCodePo fromDomain(BuildConfigFeatureCode domain);
}