package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.ConfigItemMappingVo;
import net.hwyz.iov.cloud.edd.vmd.api.vo.ConfigItemOptionVo;
import net.hwyz.iov.cloud.edd.vmd.api.vo.ConfigItemVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.ConfigItemDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.ConfigItemMappingDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.ConfigItemOptionDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

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
    ConfigItemVo fromItemDto(ConfigItemDto configItemDto);

    /**
     * 配置项 VO 转 DTO
     *
     * @param configItemVo 配置项 VO
     * @return 配置项 DTO
     */
    ConfigItemDto toItemDto(ConfigItemVo configItemVo);

    /**
     * 配置项 DTO 列表转 VO 列表
     *
     * @param configItemDtoList 配置项 DTO 列表
     * @return 配置项 VO 列表
     */
    List<ConfigItemVo> fromItemDtoList(List<ConfigItemDto> configItemDtoList);

    /**
     * 枚举值 DTO 转 VO
     *
     * @param configItemOptionDto 枚举值 DTO
     * @return 枚举值 VO
     */
    ConfigItemOptionVo fromOptionDto(ConfigItemOptionDto configItemOptionDto);

    /**
     * 枚举值 VO 转 DTO
     *
     * @param configItemOptionVo 枚举值 VO
     * @return 枚举值 DTO
     */
    ConfigItemOptionDto toOptionDto(ConfigItemOptionVo configItemOptionVo);

    /**
     * 枚举值 DTO 列表转 VO 列表
     *
     * @param configItemOptionDtoList 枚举值 DTO 列表
     * @return 枚举值 VO 列表
     */
    List<ConfigItemOptionVo> fromOptionDtoList(List<ConfigItemOptionDto> configItemOptionDtoList);

    /**
     * 映射 DTO 转 VO
     *
     * @param configItemMappingDto 映射 DTO
     * @return 映射 VO
     */
    ConfigItemMappingVo fromMappingDto(ConfigItemMappingDto configItemMappingDto);

    /**
     * 映射 VO 转 DTO
     *
     * @param configItemMappingVo 映射 VO
     * @return 映射 DTO
     */
    ConfigItemMappingDto toMappingDto(ConfigItemMappingVo configItemMappingVo);

    /**
     * 映射 DTO 列表转 VO 列表
     *
     * @param configItemMappingDtoList 映射 DTO 列表
     * @return 映射 VO 列表
     */
    List<ConfigItemMappingVo> fromMappingDtoList(List<ConfigItemMappingDto> configItemMappingDtoList);

}
