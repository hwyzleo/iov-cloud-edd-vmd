package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigItemMappingDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigItemMapping;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ConfigItemMappingCmd;

import java.util.List;

/**
 * 配置项映射 DTO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface ConfigItemMappingAssembler {

    ConfigItemMappingAssembler INSTANCE = Mappers.getMapper(ConfigItemMappingAssembler.class);

    /**
     * 领域对象转 DTO
     *
     * @param configItemMapping 领域对象
     * @return DTO
     */
    ConfigItemMappingDto fromDomain(ConfigItemMapping configItemMapping);

    /**
     * DTO 转领域对象
     *
     * @param configItemMappingDto DTO
     * @return 领域对象
     */
    ConfigItemMapping toDomain(ConfigItemMappingDto configItemMappingDto);
    /**
     * 命令转领域对象
     *
     * @param cmd 命令
     * @return 领域对象
     */
    ConfigItemMapping toDomain(ConfigItemMappingCmd cmd);


    /**
     * 领域对象列表转 DTO 列表
     *
     * @param configItemMappingList 领域对象列表
     * @return DTO 列表
     */
    List<ConfigItemMappingDto> fromDomainList(List<ConfigItemMapping> configItemMappingList);

}
