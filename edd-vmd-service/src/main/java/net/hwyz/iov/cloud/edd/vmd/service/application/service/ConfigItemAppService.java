package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.ConfigItemMappingVo;
import net.hwyz.iov.cloud.edd.vmd.api.vo.ConfigItemOptionVo;
import net.hwyz.iov.cloud.edd.vmd.api.vo.ConfigItemVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.ConfigItemAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.ConfigItemMappingAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.ConfigItemOptionAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigItem;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigItemMapping;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigItemOption;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.ConfigItemRepository;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置项应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigItemAppService {

    private final ConfigItemRepository configItemRepository;

    // ==================== 配置项 ====================

    /**
     * 查询配置项信息
     *
     * @param code      配置项编码
     * @param name      配置项名称
     * @param beginTime 开始时间
     * @param endTime    结束时间
     * @return 配置项列表
     */
    public List<ConfigItemVo> search(String code, String name, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", code);
        map.put("name", ParamHelper.fuzzyQueryParam(name));
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        List<ConfigItem> configItemList = configItemRepository.selectByMap(map);
        return PageUtil.convert(configItemList, ConfigItemAssembler.INSTANCE::fromDomain);
    }

    /**
     * 检查配置项编码是否唯一
     *
     * @param configItemId 配置项ID
     * @param code         配置项编码
     * @return 结果
     */
    public Boolean checkCodeUnique(Long configItemId, String code) {
        if (ObjUtil.isNull(configItemId)) {
            configItemId = -1L;
        }
        ConfigItem configItem = configItemRepository.selectByCode(code);
        return !ObjUtil.isNotNull(configItem) || configItem.getId().longValue() == configItemId.longValue();
    }

    /**
     * 根据主键ID获取配置项信息
     *
     * @param id 主键ID
     * @return 配置项信息
     */
    public ConfigItemVo getConfigItemById(Long id) {
        return ConfigItemAssembler.INSTANCE.fromDomain(configItemRepository.selectById(id));
    }

    /**
     * 新增配置项
     *
     * @param configItemVo 配置项信息
     * @param userId       操作用户ID
     * @return 结果
     */
    public int createConfigItem(ConfigItemVo configItemVo, String userId) {
        ConfigItem configItem = ConfigItemAssembler.INSTANCE.toDomain(configItemVo);
        configItem.setCreateBy(userId);
        return configItemRepository.insert(configItem);
    }

    /**
     * 修改配置项
     *
     * @param configItemVo 配置项信息
     * @param userId       操作用户ID
     * @return 结果
     */
    public int modifyConfigItem(ConfigItemVo configItemVo, String userId) {
        ConfigItem configItem = ConfigItemAssembler.INSTANCE.toDomain(configItemVo);
        configItem.setModifyBy(userId);
        return configItemRepository.update(configItem);
    }

    /**
     * 批量删除配置项
     *
     * @param ids 配置项ID数组
     * @return 结果
     */
    public int deleteConfigItemByIds(Long[] ids) {
        return configItemRepository.batchPhysicalDelete(ids);
    }

    // ==================== 配置项枚举值 ====================

    /**
     * 查询配置项枚举值信息
     *
     * @param configItemCode 配置项编码
     * @param code           枚举值编码
     * @param name           枚举值名称
     * @param beginTime      开始时间
     * @param endTime        结束时间
     * @return 配置项枚举值列表
     */
    public List<ConfigItemOptionVo> searchOption(String configItemCode, String code, String name, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("configItemCode", configItemCode);
        map.put("code", code);
        map.put("name", ParamHelper.fuzzyQueryParam(name));
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        List<ConfigItemOption> configItemOptionList = configItemRepository.selectOptionByMap(map);
        return PageUtil.convert(configItemOptionList, ConfigItemOptionAssembler.INSTANCE::fromDomain);
    }

    /**
     * 检查配置项枚举值编码是否唯一
     *
     * @param configItemOptionId 配置项枚举值ID
     * @param configItemCode     配置项编码
     * @param code               枚举值编码
     * @return 结果
     */
    public Boolean checkOptionCodeUnique(Long configItemOptionId, String configItemCode, String code) {
        if (ObjUtil.isNull(configItemOptionId)) {
            configItemOptionId = -1L;
        }
        ConfigItemOption configItemOption = configItemRepository.selectOptionByCode(configItemCode, code);
        return !ObjUtil.isNotNull(configItemOption) || configItemOption.getId().longValue() == configItemOptionId.longValue();
    }

    /**
     * 根据主键ID获取配置项枚举值信息
     *
     * @param configItemCode 配置项编码
     * @param id             主键ID
     * @return 配置项枚举值信息
     */
    public ConfigItemOptionVo getConfigItemOptionById(String configItemCode, Long id) {
        return ConfigItemOptionAssembler.INSTANCE.fromDomain(configItemRepository.selectOptionById(id));
    }

    /**
     * 新增配置项枚举值
     *
     * @param configItemCode     配置项编码
     * @param configItemOptionVo 配置项枚举值信息
     * @param userId             操作用户ID
     * @return 结果
     */
    public int createConfigItemOption(String configItemCode, ConfigItemOptionVo configItemOptionVo, String userId) {
        ConfigItemOption configItemOption = ConfigItemOptionAssembler.INSTANCE.toDomain(configItemOptionVo);
        configItemOption.setConfigItemCode(configItemCode);
        configItemOption.setCreateBy(userId);
        return configItemRepository.insertOption(configItemOption);
    }

    /**
     * 修改配置项枚举值
     *
     * @param configItemCode     配置项编码
     * @param configItemOptionVo 配置项枚举值信息
     * @param userId             操作用户ID
     * @return 结果
     */
    public int modifyConfigItemOption(String configItemCode, ConfigItemOptionVo configItemOptionVo, String userId) {
        ConfigItemOption configItemOption = ConfigItemOptionAssembler.INSTANCE.toDomain(configItemOptionVo);
        configItemOption.setConfigItemCode(configItemCode);
        configItemOption.setModifyBy(userId);
        return configItemRepository.updateOption(configItemOption);
    }

    /**
     * 批量删除配置项枚举值
     *
     * @param configItemCode 配置项编码
     * @param ids            配置项枚举值ID数组
     * @return 结果
     */
    public int deleteConfigItemOptionByIds(String configItemCode, Long[] ids) {
        return configItemRepository.batchPhysicalDeleteOption(ids);
    }

    // ==================== 配置项映射 ====================

    /**
     * 查询配置项映射信息
     *
     * @param configItemCode 配置项编码
     * @param sourceSystem   源系统
     * @param beginTime      开始时间
     * @param endTime        结束时间
     * @return 配置项映射列表
     */
    public List<ConfigItemMappingVo> searchMapping(String configItemCode, String sourceSystem, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("configItemCode", configItemCode);
        map.put("sourceSystem", sourceSystem);
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        List<ConfigItemMapping> configItemMappingList = configItemRepository.selectMappingByMap(map);
        return PageUtil.convert(configItemMappingList, ConfigItemMappingAssembler.INSTANCE::fromDomain);
    }

    /**
     * 检查配置项映射编码是否唯一
     *
     * @param configItemMappingId 配置项映射ID
     * @param configItemCode      配置项编码
     * @param sourceSystem        源系统
     * @param sourceCode          源系统代码
     * @param sourceValue         源系统值
     * @return 结果
     */
    public Boolean checkMappingCodeUnique(Long configItemMappingId, String configItemCode, String sourceSystem, String sourceCode, String sourceValue) {
        if (ObjUtil.isNull(configItemMappingId)) {
            configItemMappingId = -1L;
        }
        ConfigItemMapping configItemMapping = configItemRepository.selectMappingBySourceValue(configItemCode, sourceSystem, sourceCode, sourceValue);
        return !ObjUtil.isNotNull(configItemMapping) || configItemMapping.getId().longValue() == configItemMappingId.longValue();
    }

    /**
     * 根据主键ID获取配置项映射信息
     *
     * @param configItemCode 配置项编码
     * @param id             主键ID
     * @return 配置项映射信息
     */
    public ConfigItemMappingVo getConfigItemMappingById(String configItemCode, Long id) {
        return ConfigItemMappingAssembler.INSTANCE.fromDomain(configItemRepository.selectMappingById(id));
    }

    /**
     * 新增配置项映射
     *
     * @param configItemCode      配置项编码
     * @param configItemMappingVo 配置项映射信息
     * @param userId              操作用户ID
     * @return 结果
     */
    public int createConfigItemMapping(String configItemCode, ConfigItemMappingVo configItemMappingVo, String userId) {
        ConfigItemMapping configItemMapping = ConfigItemMappingAssembler.INSTANCE.toDomain(configItemMappingVo);
        configItemMapping.setConfigItemCode(configItemCode);
        configItemMapping.setCreateBy(userId);
        return configItemRepository.insertMapping(configItemMapping);
    }

    /**
     * 修改配置项映射
     *
     * @param configItemCode      配置项编码
     * @param configItemMappingVo 配置项映射信息
     * @param userId              操作用户ID
     * @return 结果
     */
    public int modifyConfigItemMapping(String configItemCode, ConfigItemMappingVo configItemMappingVo, String userId) {
        ConfigItemMapping configItemMapping = ConfigItemMappingAssembler.INSTANCE.toDomain(configItemMappingVo);
        configItemMapping.setConfigItemCode(configItemCode);
        configItemMapping.setModifyBy(userId);
        return configItemRepository.updateMapping(configItemMapping);
    }

    /**
     * 批量删除配置项映射
     *
     * @param configItemCode 配置项编码
     * @param ids            配置项映射ID数组
     * @return 结果
     */
    public int deleteConfigItemMappingByIds(String configItemCode, Long[] ids) {
        return configItemRepository.batchPhysicalDeleteMapping(ids);
    }

}
