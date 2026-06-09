package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.ConfigurationOptionCodeRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.ConfigurationRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.ConfigurationOptionCodeResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.ConfigurationResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigurationDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigurationOptionCodeDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ConfigurationCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ConfigurationOptionCodeCmd;

import java.util.List;

@Mapper
public interface MptConfigurationAssembler {

    MptConfigurationAssembler INSTANCE = Mappers.getMapper(MptConfigurationAssembler.class);

    ConfigurationResponse fromDto(ConfigurationDto configurationDto);

    ConfigurationDto toDto(ConfigurationRequest configurationVo);

    ConfigurationCmd toCmd(ConfigurationRequest vo);

    List<ConfigurationResponse> fromDtoList(List<ConfigurationDto> configurationDtoList);

    ConfigurationOptionCodeResponse fromOptionCodeDto(ConfigurationOptionCodeDto configurationOptionCodeDto);

    ConfigurationOptionCodeCmd toOptionCodeCmd(ConfigurationOptionCodeRequest configurationOptionCodeRequest);

    List<ConfigurationOptionCodeResponse> fromOptionCodeDtoList(List<ConfigurationOptionCodeDto> configurationOptionCodeDtoList);

}
