package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.ConfigItemVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.ConfigItemPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台配置项转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface ConfigItemAssembler {

    ConfigItemAssembler INSTANCE = Mappers.getMapper(ConfigItemAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param configItemPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    ConfigItemVo fromPo(ConfigItemPo configItemPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param configItemVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    ConfigItemPo toPo(ConfigItemVo configItemVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param configItemPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<ConfigItemVo> fromPoList(List<ConfigItemPo> configItemPoList);

}
