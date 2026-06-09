package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VariantFeatureCodeCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VariantFeatureCodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VariantFeatureCode;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 版本特征值 DTO 转换器（原BaseModelFeatureCodeAssembler，CR-016重命名）
 *
 * @author hwyz_leo
 */
@Mapper
public interface VariantFeatureCodeAssembler {

    VariantFeatureCodeAssembler INSTANCE = Mappers.getMapper(VariantFeatureCodeAssembler.class);

    /**
     * 领域对象转 DTO
     *
     * @param variantFeatureCode 领域对象
     * @return DTO
     */
    @Mapping(target = "featureCode", expression = "java(net.hwyz.iov.cloud.framework.common.util.StrUtil.isBlank(variantFeatureCode.getFeatureCode()) ? null : variantFeatureCode.getFeatureCode().split(\",\"))")
    @Mapping(target = "featureName", expression = "java(net.hwyz.iov.cloud.framework.common.util.StrUtil.isBlank(variantFeatureCode.getFeatureName()) ? null : variantFeatureCode.getFeatureName().split(\",\"))")
    VariantFeatureCodeDto fromDomain(VariantFeatureCode variantFeatureCode);

    /**
     * DTO 转领域对象
     *
     * @param variantFeatureCodeDto DTO
     * @return 领域对象
     */
    @Mapping(target = "featureCode", expression = "java(variantFeatureCodeDto.getFeatureCode() == null ? null : String.join(\",\", variantFeatureCodeDto.getFeatureCode()))")
    @Mapping(target = "featureName", expression = "java(variantFeatureCodeDto.getFeatureName() == null ? null : String.join(\",\", variantFeatureCodeDto.getFeatureName()))")
    VariantFeatureCode toDomain(VariantFeatureCodeDto variantFeatureCodeDto);

    /**
     * 命令转领域对象
     *
     * @param variantFeatureCodeCmd 命令
     * @return 领域对象
     */
    @Mapping(target = "featureCode", expression = "java(variantFeatureCodeCmd.getFeatureCode() == null ? null : String.join(\",\", variantFeatureCodeCmd.getFeatureCode()))")
    @Mapping(target = "featureName", expression = "java(variantFeatureCodeCmd.getFeatureName() == null ? null : String.join(\",\", variantFeatureCodeCmd.getFeatureName()))")
    VariantFeatureCode toDomain(VariantFeatureCodeCmd variantFeatureCodeCmd);

    /**
     * 领域对象列表转 DTO 列表
     *
     * @param variantFeatureCodeList 领域对象列表
     * @return DTO 列表
     */
    List<VariantFeatureCodeDto> fromDomainList(List<VariantFeatureCode> variantFeatureCodeList);

}