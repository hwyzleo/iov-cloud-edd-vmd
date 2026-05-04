package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.BuildConfigFeatureCodeRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.BuildConfigRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.BuildConfigFeatureCodeResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.BuildConfigResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BuildConfigDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BuildConfigFeatureCodeDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.BuildConfigCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.BuildConfigFeatureCodeCmd;

import java.util.List;

@Mapper
public interface MptBuildConfigAssembler {

    MptBuildConfigAssembler INSTANCE = Mappers.getMapper(MptBuildConfigAssembler.class);

    BuildConfigResponse fromDto(BuildConfigDto buildConfigDto);

    BuildConfigDto toDto(BuildConfigRequest buildConfigVo);

    BuildConfigCmd toCmd(BuildConfigRequest vo);

    List<BuildConfigResponse> fromDtoList(List<BuildConfigDto> buildConfigDtoList);

    BuildConfigFeatureCodeResponse fromFeatureCodeDto(BuildConfigFeatureCodeDto buildConfigFeatureCodeDto);

    BuildConfigFeatureCodeCmd toFeatureCodeCmd(BuildConfigFeatureCodeRequest buildConfigFeatureCodeRequest);

    List<BuildConfigFeatureCodeResponse> fromFeatureCodeDtoList(List<BuildConfigFeatureCodeDto> buildConfigFeatureCodeDtoList);

}