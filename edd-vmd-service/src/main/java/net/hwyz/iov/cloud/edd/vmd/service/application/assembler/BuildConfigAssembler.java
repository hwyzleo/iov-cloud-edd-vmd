package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BuildConfigDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Configuration;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.BuildConfigCmd;

import java.util.List;

/**
 * 生产配置 DTO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface BuildConfigAssembler {

    BuildConfigAssembler INSTANCE = Mappers.getMapper(BuildConfigAssembler.class);

    /**
     * 领域对象转 DTO
     *
     * @param configuration 领域对象
     * @return DTO
     */
    BuildConfigDto fromDomain(Configuration configuration);

    /**
     * DTO 转领域对象
     *
     * @param buildConfigDto DTO
     * @return 领域对象
     */
    Configuration toDomain(BuildConfigDto buildConfigDto);
    /**
     * 命令转领域对象
     *
     * @param cmd 命令
     * @return 领域对象
     */
    Configuration toDomain(BuildConfigCmd cmd);


    /**
     * 领域对象列表转 DTO 列表
     *
     * @param configurationList 领域对象列表
     * @return DTO 列表
     */
    List<BuildConfigDto> fromDomainList(List<Configuration> configurationList);

}
