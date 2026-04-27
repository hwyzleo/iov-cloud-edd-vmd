package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigItemOptionDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigItemOption;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ConfigItemOptionCmd;

import java.util.List;

/**
 * 配置项枚举值 DTO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface ConfigItemOptionAssembler {

    ConfigItemOptionAssembler INSTANCE = Mappers.getMapper(ConfigItemOptionAssembler.class);

    /**
     * 领域对象转 DTO
     *
     * @param configItemOption 领域对象
     * @return DTO
     */
    ConfigItemOptionDto fromDomain(ConfigItemOption configItemOption);

    /**
     * DTO 转领域对象
     *
     * @param configItemOptionDto DTO
     * @return 领域对象
     */
    ConfigItemOption toDomain(ConfigItemOptionDto configItemOptionDto);
    /**
     * 命令转领域对象
     *
     * @param cmd 命令
     * @return 领域对象
     */
    ConfigItemOption toDomain(ConfigItemOptionCmd cmd);


    /**
     * 领域对象列表转 DTO 列表
     *
     * @param configItemOptionList 领域对象列表
     * @return DTO 列表
     */
    List<ConfigItemOptionDto> fromDomainList(List<ConfigItemOption> configItemOptionList);

}
