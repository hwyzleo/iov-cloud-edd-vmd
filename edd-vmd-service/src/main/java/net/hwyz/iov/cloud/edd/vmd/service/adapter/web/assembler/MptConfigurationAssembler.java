package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.ConfigurationFeatureCodeRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.ConfigurationRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.ConfigurationFeatureCodeResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.ConfigurationResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigurationDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigurationFeatureCodeDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ConfigurationCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ConfigurationFeatureCodeCmd;

import java.util.List;

@Mapper
public interface MptConfigurationAssembler {

    MptConfigurationAssembler INSTANCE = Mappers.getMapper(MptConfigurationAssembler.class);

    ConfigurationResponse fromDto(ConfigurationDto configurationDto);

    ConfigurationDto toDto(ConfigurationRequest configurationVo);

    ConfigurationCmd toCmd(ConfigurationRequest vo);

    List<ConfigurationResponse> fromDtoList(List<ConfigurationDto> configurationDtoList);

    ConfigurationFeatureCodeResponse fromFeatureCodeDto(ConfigurationFeatureCodeDto configurationFeatureCodeDto);

    ConfigurationFeatureCodeCmd toFeatureCodeCmd(ConfigurationFeatureCodeRequest configurationFeatureCodeRequest);

    List<ConfigurationFeatureCodeResponse> fromFeatureCodeDtoList(List<ConfigurationFeatureCodeDto> configurationFeatureCodeDtoList);

}
