package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VmdBuildConfigResponse;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VmdBuildConfigOptionCodeResponse;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VmdConfigurationOptionCodeResponse;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VmdConfigurationResponse;
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

    /**
     * DTO 转配置响应（CR-047：基础字段 + 产品树派生字段）
     *
     * @param dto 配置 DTO
     * @return 配置响应
     */
    VmdConfigurationResponse toConfigurationResponse(ConfigurationDto dto);

    /**
     * DTO 列表转配置响应列表
     *
     * @param dtoList 配置 DTO 列表
     * @return 配置响应列表
     */
    List<VmdConfigurationResponse> toConfigurationResponseList(List<ConfigurationDto> dtoList);

    /**
     * 配置选项值 DTO 转配置选项值响应
     *
     * @param dto 配置选项值 DTO
     * @return 配置选项值响应
     */
    VmdConfigurationOptionCodeResponse toConfigurationOptionCodeResponse(ConfigurationOptionCodeDto dto);

    /**
     * 配置选项值 DTO 列表转配置选项值响应列表
     *
     * @param dtoList 配置选项值 DTO 列表
     * @return 配置选项值响应列表
     */
    List<VmdConfigurationOptionCodeResponse> toConfigurationOptionCodeResponseList(List<ConfigurationOptionCodeDto> dtoList);

}
