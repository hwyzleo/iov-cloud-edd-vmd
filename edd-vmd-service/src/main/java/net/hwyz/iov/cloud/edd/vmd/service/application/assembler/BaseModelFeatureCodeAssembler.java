package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.BaseModelFeatureCodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BaseModelFeatureCode;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 基础车型特征值 DTO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface BaseModelFeatureCodeAssembler {

    BaseModelFeatureCodeAssembler INSTANCE = Mappers.getMapper(BaseModelFeatureCodeAssembler.class);

    /**
     * 领域对象转 DTO
     *
     * @param baseModelFeatureCode 领域对象
     * @return DTO
     */
    @Mapping(target = "featureCode", expression = "java(net.hwyz.iov.cloud.framework.common.util.StrUtil.isBlank(baseModelFeatureCode.getFeatureCode()) ? null : baseModelFeatureCode.getFeatureCode().split(\",\"))")
    @Mapping(target = "featureName", expression = "java(net.hwyz.iov.cloud.framework.common.util.StrUtil.isBlank(baseModelFeatureCode.getFeatureName()) ? null : baseModelFeatureCode.getFeatureName().split(\",\"))")
    BaseModelFeatureCodeDto fromDomain(BaseModelFeatureCode baseModelFeatureCode);

    /**
     * DTO 转领域对象
     *
     * @param baseModelFeatureCodeDto DTO
     * @return 领域对象
     */
    @Mapping(target = "featureCode", expression = "java(baseModelFeatureCodeDto.getFeatureCode() == null ? null : String.join(\",\", baseModelFeatureCodeDto.getFeatureCode()))")
    @Mapping(target = "featureName", expression = "java(baseModelFeatureCodeDto.getFeatureName() == null ? null : String.join(\",\", baseModelFeatureCodeDto.getFeatureName()))")
    BaseModelFeatureCode toDomain(BaseModelFeatureCodeDto baseModelFeatureCodeDto);

    /**
     * 领域对象列表转 DTO 列表
     *
     * @param baseModelFeatureCodeList 领域对象列表
     * @return DTO 列表
     */
    List<BaseModelFeatureCodeDto> fromDomainList(List<BaseModelFeatureCode> baseModelFeatureCodeList);

}
