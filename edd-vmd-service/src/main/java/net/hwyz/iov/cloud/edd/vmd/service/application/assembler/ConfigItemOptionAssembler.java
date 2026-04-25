package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.ConfigItemOptionVo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigItemOption;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台配置项枚举值转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface ConfigItemOptionAssembler {

    ConfigItemOptionAssembler INSTANCE = Mappers.getMapper(ConfigItemOptionAssembler.class);

    /**
     * 领域对象转数据传输对象
     *
     * @param configItemOption 领域对象
     * @return 数据传输对象
     */
    ConfigItemOptionVo fromDomain(ConfigItemOption configItemOption);

    /**
     * 数据传输对象转领域对象
     *
     * @param configItemOptionVo 数据传输对象
     * @return 领域对象
     */
    ConfigItemOption toDomain(ConfigItemOptionVo configItemOptionVo);

    /**
     * 领域对象列表转数据传输对象列表
     *
     * @param configItemOptionList 领域对象列表
     * @return 数据传输对象列表
     */
    List<ConfigItemOptionVo> fromDomainList(List<ConfigItemOption> configItemOptionList);

}
