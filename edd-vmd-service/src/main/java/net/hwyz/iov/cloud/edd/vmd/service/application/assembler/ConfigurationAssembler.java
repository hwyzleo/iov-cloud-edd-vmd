package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigurationDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Configuration;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ConfigurationCmd;

import java.util.List;

/**
 * 生产配置 DTO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface ConfigurationAssembler {

    ConfigurationAssembler INSTANCE = Mappers.getMapper(ConfigurationAssembler.class);

    /**
     * 领域对象转 DTO
     *
     * @param configuration 领域对象
     * @return DTO
     */
    ConfigurationDto fromDomain(Configuration configuration);

    /**
     * DTO 转领域对象
     *
     * @param configurationDto DTO
     * @return 领域对象
     */
    Configuration toDomain(ConfigurationDto configurationDto);
    /**
     * 命令转领域对象
     *
     * @param cmd 命令
     * @return 领域对象
     */
    Configuration toDomain(ConfigurationCmd cmd);


    /**
     * 领域对象列表转 DTO 列表
     *
     * @param configurationList 领域对象列表
     * @return DTO 列表
     */
    List<ConfigurationDto> fromDomainList(List<Configuration> configurationList);

}
