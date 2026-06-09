package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ConfigurationOptionCodeCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigurationOptionCodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigurationOptionCode;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 配置选项值 DTO 转换器（原BuildConfigFeatureCodeAssembler→ConfigurationFeatureCodeAssembler，CR-018重命名）
 *
 * @author hwyz_leo
 */
@Mapper
public interface ConfigurationOptionCodeAssembler {

    ConfigurationOptionCodeAssembler INSTANCE = Mappers.getMapper(ConfigurationOptionCodeAssembler.class);

    @Mapping(target = "configurationCode", source = "configurationCode")
    @Mapping(target = "optionCode", expression = "java(net.hwyz.iov.cloud.framework.common.util.StrUtil.isBlank(configurationOptionCode.getOptionCode()) ? null : configurationOptionCode.getOptionCode().split(\",\"))")
    @Mapping(target = "optionName", expression = "java(net.hwyz.iov.cloud.framework.common.util.StrUtil.isBlank(configurationOptionCode.getOptionName()) ? null : configurationOptionCode.getOptionName().split(\",\"))")
    @Mapping(target = "optionFamilyName", ignore = true)
    @Mapping(target = "description", ignore = true)
    ConfigurationOptionCodeDto fromDomain(ConfigurationOptionCode configurationOptionCode);

    @Mapping(target = "configurationCode", source = "configurationCode")
    @Mapping(target = "optionCode", expression = "java(configurationOptionCodeDto.getOptionCode() == null ? null : String.join(\",\", configurationOptionCodeDto.getOptionCode()))")
    @Mapping(target = "optionName", expression = "java(configurationOptionCodeDto.getOptionName() == null ? null : String.join(\",\", configurationOptionCodeDto.getOptionName()))")
    ConfigurationOptionCode toDomain(ConfigurationOptionCodeDto configurationOptionCodeDto);

    @Mapping(target = "configurationCode", source = "configurationCode")
    @Mapping(target = "optionCode", expression = "java(configurationOptionCodeCmd.getOptionCode() == null ? null : String.join(\",\", configurationOptionCodeCmd.getOptionCode()))")
    @Mapping(target = "optionName", expression = "java(configurationOptionCodeCmd.getOptionName() == null ? null : String.join(\",\", configurationOptionCodeCmd.getOptionName()))")
    ConfigurationOptionCode toDomain(ConfigurationOptionCodeCmd configurationOptionCodeCmd);

    List<ConfigurationOptionCodeDto> fromDomainList(List<ConfigurationOptionCode> configurationOptionCodeList);

}
