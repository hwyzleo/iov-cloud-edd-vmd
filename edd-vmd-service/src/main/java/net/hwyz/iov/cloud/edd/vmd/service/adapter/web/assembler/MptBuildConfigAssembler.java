package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.BuildConfigFeatureCodeRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.BuildConfigRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.BuildConfigFeatureCodeResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.BuildConfigResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigurationDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigurationFeatureCodeDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ConfigurationCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ConfigurationFeatureCodeCmd;

import java.util.List;

@Mapper
public interface MptBuildConfigAssembler {

    MptBuildConfigAssembler INSTANCE = Mappers.getMapper(MptBuildConfigAssembler.class);

    BuildConfigResponse fromDto(ConfigurationDto configurationDto);

    ConfigurationDto toDto(BuildConfigRequest buildConfigVo);

    ConfigurationCmd toCmd(BuildConfigRequest vo);

    List<BuildConfigResponse> fromDtoList(List<ConfigurationDto> configurationDtoList);

    BuildConfigFeatureCodeResponse fromFeatureCodeDto(ConfigurationFeatureCodeDto configurationFeatureCodeDto);

    ConfigurationFeatureCodeCmd toFeatureCodeCmd(BuildConfigFeatureCodeRequest buildConfigFeatureCodeRequest);

    List<BuildConfigFeatureCodeResponse> fromFeatureCodeDtoList(List<ConfigurationFeatureCodeDto> configurationFeatureCodeDtoList);

}