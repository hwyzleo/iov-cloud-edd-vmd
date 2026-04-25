package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigItem;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigItemMapping;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigItemOption;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.ConfigItemMappingPo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.ConfigItemOptionPo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.ConfigItemPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 配置项相关领域对象转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface ConfigItemConverter {

    ConfigItemConverter INSTANCE = Mappers.getMapper(ConfigItemConverter.class);

    // ==================== 配置项 ====================

    @Mapping(target = "state", ignore = true)
    ConfigItem toDomain(ConfigItemPo po);

    List<ConfigItem> toDomainList(List<ConfigItemPo> poList);

    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "rowValid", ignore = true)
    ConfigItemPo fromDomain(ConfigItem domain);

    // ==================== 配置项枚举值 ====================

    @Mapping(target = "state", ignore = true)
    ConfigItemOption toOptionDomain(ConfigItemOptionPo po);

    List<ConfigItemOption> toOptionDomainList(List<ConfigItemOptionPo> poList);

    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "rowValid", ignore = true)
    ConfigItemOptionPo fromOptionDomain(ConfigItemOption domain);

    // ==================== 配置项映射 ====================

    @Mapping(target = "state", ignore = true)
    ConfigItemMapping toMappingDomain(ConfigItemMappingPo po);

    List<ConfigItemMapping> toMappingDomainList(List<ConfigItemMappingPo> poList);

    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "rowValid", ignore = true)
    ConfigItemMappingPo fromMappingDomain(ConfigItemMapping domain);
}
