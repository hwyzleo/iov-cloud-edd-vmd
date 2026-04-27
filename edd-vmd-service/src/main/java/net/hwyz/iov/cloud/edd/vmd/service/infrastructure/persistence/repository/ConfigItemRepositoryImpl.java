package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigItem;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigItemMapping;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigItemOption;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.ConfigItemRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.ConfigItemConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.ConfigItemMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.ConfigItemMappingMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.ConfigItemOptionMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.ConfigItemMappingPo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.ConfigItemOptionPo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.ConfigItemPo;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 配置项数据仓库接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ConfigItemRepositoryImpl implements ConfigItemRepository {

    private final ConfigItemMapper configItemMapper;
    private final ConfigItemOptionMapper configItemOptionMapper;
    private final ConfigItemMappingMapper configItemMappingMapper;

    // ==================== 配置项 ====================

    @Override
    public List<ConfigItem> selectByMap(Map<String, Object> map) {
        List<ConfigItemPo> poList = configItemMapper.selectPoByMap(map);
        return PageUtil.convert(poList, ConfigItemConverter.INSTANCE::toDomain);
    }

    @Override
    public ConfigItem selectById(Long id) {
        return ConfigItemConverter.INSTANCE.toDomain(configItemMapper.selectPoById(id));
    }

    @Override
    public ConfigItem selectByCode(String code) {
        return ConfigItemConverter.INSTANCE.toDomain(configItemMapper.selectPoByCode(code));
    }

    @Override
    public int insert(ConfigItem configItem) {
        return configItemMapper.insertPo(ConfigItemConverter.INSTANCE.fromDomain(configItem));
    }

    @Override
    public int update(ConfigItem configItem) {
        return configItemMapper.updatePo(ConfigItemConverter.INSTANCE.fromDomain(configItem));
    }

    @Override
    public int batchPhysicalDelete(Long[] ids) {
        return configItemMapper.batchPhysicalDeletePo(ids);
    }

    // ==================== 配置项枚举值 ====================

    @Override
    public List<ConfigItemOption> selectOptionByMap(Map<String, Object> map) {
        List<ConfigItemOptionPo> poList = configItemOptionMapper.selectPoByMap(map);
        return PageUtil.convert(poList, ConfigItemConverter.INSTANCE::toOptionDomain);
    }

    @Override
    public ConfigItemOption selectOptionById(Long id) {
        return ConfigItemConverter.INSTANCE.toOptionDomain(configItemOptionMapper.selectPoById(id));
    }

    @Override
    public ConfigItemOption selectOptionByCode(String configItemCode, String code) {
        return ConfigItemConverter.INSTANCE.toOptionDomain(configItemOptionMapper.selectPoByCode(configItemCode, code));
    }

    @Override
    public int insertOption(ConfigItemOption configItemOption) {
        return configItemOptionMapper.insertPo(ConfigItemConverter.INSTANCE.fromOptionDomain(configItemOption));
    }

    @Override
    public int updateOption(ConfigItemOption configItemOption) {
        return configItemOptionMapper.updatePo(ConfigItemConverter.INSTANCE.fromOptionDomain(configItemOption));
    }

    @Override
    public int batchPhysicalDeleteOption(Long[] ids) {
        return configItemOptionMapper.batchPhysicalDeletePo(ids);
    }

    // ==================== 配置项映射 ====================

    @Override
    public List<ConfigItemMapping> selectMappingByMap(Map<String, Object> map) {
        List<ConfigItemMappingPo> poList = configItemMappingMapper.selectPoByMap(map);
        return PageUtil.convert(poList, ConfigItemConverter.INSTANCE::toMappingDomain);
    }

    @Override
    public ConfigItemMapping selectMappingById(Long id) {
        return ConfigItemConverter.INSTANCE.toMappingDomain(configItemMappingMapper.selectPoById(id));
    }

    @Override
    public ConfigItemMapping selectMappingBySourceCode(String configItemCode, String sourceSystem, String sourceCode) {
        return ConfigItemConverter.INSTANCE.toMappingDomain(configItemMappingMapper.selectPoBySourceCode(configItemCode, sourceSystem, sourceCode));
    }

    @Override
    public ConfigItemMapping selectMappingBySourceValue(String configItemCode, String sourceSystem, String sourceCode, String sourceValue) {
        return ConfigItemConverter.INSTANCE.toMappingDomain(configItemMappingMapper.selectPoBySourceValue(configItemCode, sourceSystem, sourceCode, sourceValue));
    }

    @Override
    public int insertMapping(ConfigItemMapping configItemMapping) {
        return configItemMappingMapper.insertPo(ConfigItemConverter.INSTANCE.fromMappingDomain(configItemMapping));
    }

    @Override
    public int updateMapping(ConfigItemMapping configItemMapping) {
        return configItemMappingMapper.updatePo(ConfigItemConverter.INSTANCE.fromMappingDomain(configItemMapping));
    }

    @Override
    public int batchPhysicalDeleteMapping(Long[] ids) {
        return configItemMappingMapper.batchPhysicalDeletePo(ids);
    }

}
