package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VariantOptionCodeCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VariantOptionCodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VariantOptionCode;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 版本选项值 DTO 转换器（原BaseModelFeatureCodeAssembler→VariantFeatureCodeAssembler，CR-018重命名）
 *
 * @author hwyz_leo
 */
@Mapper
public interface VariantOptionCodeAssembler {

    VariantOptionCodeAssembler INSTANCE = Mappers.getMapper(VariantOptionCodeAssembler.class);

    /**
     * 领域对象转 DTO
     *
     * @param variantOptionCode 领域对象
     * @return DTO
     */
    @Mapping(target = "optionCode", expression = "java(net.hwyz.iov.cloud.framework.common.util.StrUtil.isBlank(variantOptionCode.getOptionCode()) ? null : variantOptionCode.getOptionCode().split(\",\"))")
    @Mapping(target = "optionName", expression = "java(net.hwyz.iov.cloud.framework.common.util.StrUtil.isBlank(variantOptionCode.getOptionName()) ? null : variantOptionCode.getOptionName().split(\",\"))")
    VariantOptionCodeDto fromDomain(VariantOptionCode variantOptionCode);

    /**
     * DTO 转领域对象
     *
     * @param variantOptionCodeDto DTO
     * @return 领域对象
     */
    @Mapping(target = "optionCode", expression = "java(variantOptionCodeDto.getOptionCode() == null ? null : String.join(\",\", variantOptionCodeDto.getOptionCode()))")
    @Mapping(target = "optionName", expression = "java(variantOptionCodeDto.getOptionName() == null ? null : String.join(\",\", variantOptionCodeDto.getOptionName()))")
    VariantOptionCode toDomain(VariantOptionCodeDto variantOptionCodeDto);

    /**
     * 命令转领域对象
     *
     * @param variantOptionCodeCmd 命令
     * @return 领域对象
     */
    @Mapping(target = "optionCode", expression = "java(variantOptionCodeCmd.getOptionCode() == null ? null : String.join(\",\", variantOptionCodeCmd.getOptionCode()))")
    @Mapping(target = "optionName", expression = "java(variantOptionCodeCmd.getOptionName() == null ? null : String.join(\",\", variantOptionCodeCmd.getOptionName()))")
    VariantOptionCode toDomain(VariantOptionCodeCmd variantOptionCodeCmd);

    /**
     * 领域对象列表转 DTO 列表
     *
     * @param variantOptionCodeList 领域对象列表
     * @return DTO 列表
     */
    List<VariantOptionCodeDto> fromDomainList(List<VariantOptionCode> variantOptionCodeList);

}
