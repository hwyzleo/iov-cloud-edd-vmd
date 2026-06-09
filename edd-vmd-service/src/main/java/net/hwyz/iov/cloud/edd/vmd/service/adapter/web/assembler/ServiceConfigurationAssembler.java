package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VmdBuildConfigResponse;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VmdBuildConfigOptionCodeResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigurationDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigurationOptionCodeDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface ServiceConfigurationAssembler {

    ServiceConfigurationAssembler INSTANCE = Mappers.getMapper(ServiceConfigurationAssembler.class);

    VmdBuildConfigResponse toExResponse(ConfigurationDto dto);

    VmdBuildConfigOptionCodeResponse toOptionCodeExResponse(ConfigurationOptionCodeDto dto);

    List<VmdBuildConfigResponse> toExResponseList(List<ConfigurationDto> dtoList);

    List<VmdBuildConfigOptionCodeResponse> toOptionCodeExResponseList(List<ConfigurationOptionCodeDto> dtoList);

}
