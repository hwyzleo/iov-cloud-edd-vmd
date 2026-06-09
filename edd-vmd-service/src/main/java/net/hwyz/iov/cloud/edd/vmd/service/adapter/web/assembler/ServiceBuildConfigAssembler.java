package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VmdBuildConfigResponse;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VmdBuildConfigFeatureCodeResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigurationDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigurationFeatureCodeDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface ServiceBuildConfigAssembler {

    ServiceBuildConfigAssembler INSTANCE = Mappers.getMapper(ServiceBuildConfigAssembler.class);

    VmdBuildConfigResponse toExResponse(ConfigurationDto dto);

    VmdBuildConfigFeatureCodeResponse toFeatureCodeExResponse(ConfigurationFeatureCodeDto dto);

    List<VmdBuildConfigResponse> toExResponseList(List<ConfigurationDto> dtoList);

    List<VmdBuildConfigFeatureCodeResponse> toFeatureCodeExResponseList(List<ConfigurationFeatureCodeDto> dtoList);

}