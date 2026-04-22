package net.hwyz.iov.cloud.edd.vmd.service.application.mapper;

import net.hwyz.iov.cloud.edd.vmd.api.vo.ConfigItemVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdConfigItemDo;
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
public interface ConfigItemMapper {

    ConfigItemMapper INSTANCE = Mappers.getMapper(ConfigItemMapper.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param configItemDo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    ConfigItemVo fromDo(VmdConfigItemDo configItemDo);

    /**
     * 数据传输对象转数据对象
     *
     * @param configItemVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    VmdConfigItemDo toDo(ConfigItemVo configItemVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param configItemDoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<ConfigItemVo> fromDoList(List<VmdConfigItemDo> configItemDoList);

}
