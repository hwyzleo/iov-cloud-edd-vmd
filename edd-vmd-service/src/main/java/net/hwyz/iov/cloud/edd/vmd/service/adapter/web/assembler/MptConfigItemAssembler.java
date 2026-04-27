package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.ConfigItemMappingRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.ConfigItemMappingResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.ConfigItemOptionRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.ConfigItemOptionResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.ConfigItemRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.ConfigItemResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigItemDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigItemMappingDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigItemOptionDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ConfigItemCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ConfigItemOptionCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ConfigItemMappingCmd;

import java.util.List;

/**
 * 管理后台配置项 VO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface MptConfigItemAssembler {

    MptConfigItemAssembler INSTANCE = Mappers.getMapper(MptConfigItemAssembler.class);

    /**
     * 配置项 DTO 转 VO
     *
     * @param configItemDto 配置项 DTO
     * @return 配置项 VO
     */
    ConfigItemResponse fromItemDto(ConfigItemDto configItemDto);

    /**
     * 配置项 VO 转 DTO
     *
     * @param configItemVo 配置项 VO
     * @return 配置项 DTO
     */
    ConfigItemDto toItemDto(ConfigItemRequest configItemVo);
    ConfigItemCmd toItemCmd(ConfigItemRequest vo);


    /**
     * 配置项 DTO 列表转 VO 列表
     *
     * @param configItemDtoList 配置项 DTO 列表
     * @return 配置项 VO 列表
     */
    List<ConfigItemResponse> fromItemDtoList(List<ConfigItemDto> configItemDtoList);

    /**
     * 枚举值 DTO 转 VO
     *
     * @param configItemOptionDto 枚举值 DTO
     * @return 枚举值 VO
     */
    ConfigItemOptionResponse fromOptionDto(ConfigItemOptionDto configItemOptionDto);

    /**
     * 枚举值 VO 转 DTO
     *
     * @param configItemOptionVo 枚举值 VO
     * @return 枚举值 DTO
     */
    ConfigItemOptionDto toOptionDto(ConfigItemOptionRequest configItemOptionVo);
    ConfigItemOptionCmd toOptionCmd(ConfigItemOptionRequest vo);


    /**
     * 枚举值 DTO 列表转 VO 列表
     *
     * @param configItemOptionDtoList 枚举值 DTO 列表
     * @return 枚举值 VO 列表
     */
    List<ConfigItemOptionResponse> fromOptionDtoList(List<ConfigItemOptionDto> configItemOptionDtoList);

    /**
     * 映射 DTO 转 VO
     *
     * @param configItemMappingDto 映射 DTO
     * @return 映射 VO
     */
    ConfigItemMappingResponse fromMappingDto(ConfigItemMappingDto configItemMappingDto);

    /**
     * 映射 VO 转 DTO
     *
     * @param configItemMappingVo 映射 VO
     * @return 映射 DTO
     */
    ConfigItemMappingDto toMappingDto(ConfigItemMappingRequest configItemMappingVo);
    ConfigItemMappingCmd toMappingCmd(ConfigItemMappingRequest vo);


    /**
     * 映射 DTO 列表转 VO 列表
     *
     * @param configItemMappingDtoList 映射 DTO 列表
     * @return 映射 VO 列表
     */
    List<ConfigItemMappingResponse> fromMappingDtoList(List<ConfigItemMappingDto> configItemMappingDtoList);

}
