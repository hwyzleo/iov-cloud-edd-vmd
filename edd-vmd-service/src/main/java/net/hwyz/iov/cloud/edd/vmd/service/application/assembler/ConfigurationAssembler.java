package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigurationDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Configuration;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.ConfigurationHierarchy;
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
     * 领域对象转 DTO（基础字段，派生层级字段为空）
     *
     * @param configuration 领域对象
     * @return DTO
     */
    ConfigurationDto fromDomain(Configuration configuration);

    /**
     * 产品树补全视图转 DTO（基础字段 + 派生层级字段，CR-047）
     *
     * @param hierarchy 配置产品树补全值对象
     * @return DTO
     */
    ConfigurationDto fromHierarchy(ConfigurationHierarchy hierarchy);

    /**
     * 产品树补全视图列表转 DTO 列表
     *
     * @param hierarchyList 配置产品树补全值对象列表
     * @return DTO 列表
     */
    List<ConfigurationDto> fromHierarchyList(List<ConfigurationHierarchy> hierarchyList);

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
