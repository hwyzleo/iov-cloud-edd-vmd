package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.ConfigItemOptionVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.ConfigItemOptionPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
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
     * 数据对象转数据传输对象
     *
     * @param configItemOptionPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    ConfigItemOptionVo fromPo(ConfigItemOptionPo configItemOptionPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param configItemOptionVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    ConfigItemOptionPo toPo(ConfigItemOptionVo configItemOptionVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param configItemOptionPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<ConfigItemOptionVo> fromPoList(List<ConfigItemOptionPo> configItemOptionPoList);

}
