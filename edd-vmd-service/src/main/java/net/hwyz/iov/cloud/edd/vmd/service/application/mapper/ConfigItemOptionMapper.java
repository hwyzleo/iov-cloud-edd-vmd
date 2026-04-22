package net.hwyz.iov.cloud.edd.vmd.service.application.mapper;

import net.hwyz.iov.cloud.edd.vmd.api.vo.ConfigItemOptionVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdConfigItemOptionDo;
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
public interface ConfigItemOptionMapper {

    ConfigItemOptionMapper INSTANCE = Mappers.getMapper(ConfigItemOptionMapper.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param configItemOptionDo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    ConfigItemOptionVo fromDo(VmdConfigItemOptionDo configItemOptionDo);

    /**
     * 数据传输对象转数据对象
     *
     * @param configItemOptionVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    VmdConfigItemOptionDo toDo(ConfigItemOptionVo configItemOptionVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param configItemOptionDoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<ConfigItemOptionVo> fromDoList(List<VmdConfigItemOptionDo> configItemOptionDoList);

}
