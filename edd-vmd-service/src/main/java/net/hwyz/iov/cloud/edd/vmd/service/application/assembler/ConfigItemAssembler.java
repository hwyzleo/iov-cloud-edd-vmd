package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.ConfigItemVo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigItem;
import org.mapstruct.Mapper;
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
     * 领域对象转数据传输对象
     *
     * @param configItem 领域对象
     * @return 数据传输对象
     */
    ConfigItemVo fromDomain(ConfigItem configItem);

    /**
     * 数据传输对象转领域对象
     *
     * @param configItemVo 数据传输对象
     * @return 领域对象
     */
    ConfigItem toDomain(ConfigItemVo configItemVo);

    /**
     * 领域对象列表转数据传输对象列表
     *
     * @param configItemList 领域对象列表
     * @return 数据传输对象列表
     */
    List<ConfigItemVo> fromDomainList(List<ConfigItem> configItemList);

}
