package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.ConfigItemMappingVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.ConfigItemMappingPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台配置项映射转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface ConfigItemMappingAssembler {

    ConfigItemMappingAssembler INSTANCE = Mappers.getMapper(ConfigItemMappingAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param configItemMappingPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    ConfigItemMappingVo fromPo(ConfigItemMappingPo configItemMappingPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param configItemMappingVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    ConfigItemMappingPo toPo(ConfigItemMappingVo configItemMappingVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param configItemMappingPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<ConfigItemMappingVo> fromPoList(List<ConfigItemMappingPo> configItemMappingPoList);

}
