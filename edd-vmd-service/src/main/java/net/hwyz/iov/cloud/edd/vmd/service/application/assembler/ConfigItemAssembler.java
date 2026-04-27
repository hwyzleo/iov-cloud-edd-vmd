package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigItemDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigItem;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ConfigItemCmd;

import java.util.List;

/**
 * 配置项 DTO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface ConfigItemAssembler {

    ConfigItemAssembler INSTANCE = Mappers.getMapper(ConfigItemAssembler.class);

    /**
     * 领域对象转 DTO
     *
     * @param configItem 领域对象
     * @return DTO
     */
    ConfigItemDto fromDomain(ConfigItem configItem);

    /**
     * DTO 转领域对象
     *
     * @param configItemDto DTO
     * @return 领域对象
     */
    ConfigItem toDomain(ConfigItemDto configItemDto);
    /**
     * 命令转领域对象
     *
     * @param cmd 命令
     * @return 领域对象
     */
    ConfigItem toDomain(ConfigItemCmd cmd);


    /**
     * 领域对象列表转 DTO 列表
     *
     * @param configItemList 领域对象列表
     * @return DTO 列表
     */
    List<ConfigItemDto> fromDomainList(List<ConfigItem> configItemList);

}
