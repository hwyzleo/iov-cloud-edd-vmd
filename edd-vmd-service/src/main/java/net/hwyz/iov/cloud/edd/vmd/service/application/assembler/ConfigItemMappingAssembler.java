package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.ConfigItemMappingVo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigItemMapping;
import org.mapstruct.Mapper;
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
     * 领域对象转数据传输对象
     *
     * @param configItemMapping 领域对象
     * @return 数据传输对象
     */
    ConfigItemMappingVo fromDomain(ConfigItemMapping configItemMapping);

    /**
     * 数据传输对象转领域对象
     *
     * @param configItemMappingVo 数据传输对象
     * @return 领域对象
     */
    ConfigItemMapping toDomain(ConfigItemMappingVo configItemMappingVo);

    /**
     * 领域对象列表转数据传输对象列表
     *
     * @param configItemMappingList 领域对象列表
     * @return 数据传输对象列表
     */
    List<ConfigItemMappingVo> fromDomainList(List<ConfigItemMapping> configItemMappingList);

}
