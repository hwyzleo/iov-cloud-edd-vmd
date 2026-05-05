package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VmdBuildConfigResponse;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VmdBuildConfigFeatureCodeResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BuildConfigDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BuildConfigFeatureCodeDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface ServiceBuildConfigAssembler {

    ServiceBuildConfigAssembler INSTANCE = Mappers.getMapper(ServiceBuildConfigAssembler.class);

    VmdBuildConfigResponse toExResponse(BuildConfigDto dto);

    VmdBuildConfigFeatureCodeResponse toFeatureCodeExResponse(BuildConfigFeatureCodeDto dto);

    List<VmdBuildConfigResponse> toExResponseList(List<BuildConfigDto> dtoList);

    List<VmdBuildConfigFeatureCodeResponse> toFeatureCodeExResponseList(List<BuildConfigFeatureCodeDto> dtoList);

}