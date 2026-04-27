package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.ConfigItemAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.ConfigItemMappingAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.ConfigItemOptionAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigItemDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigItemMappingDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigItemOptionDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.ConfigItemQuery;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigItem;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigItemMapping;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigItemOption;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.ConfigItemRepository;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ConfigItemCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ConfigItemOptionCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ConfigItemMappingCmd;

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
     * @param query 查询 DTO
     * @return 配置项列表
     */
    public List<ConfigItemDto> search(ConfigItemQuery query) {
        Map<String, Object> map = new HashMap<>();
        map.put("family", query.getFamily());
        map.put("code", query.getCode());
        map.put("name", ParamHelper.fuzzyQueryParam(query.getName()));
        map.put("beginTime", query.getBeginTime());
        map.put("endTime", query.getEndTime());
        List<ConfigItem> configItemList = configItemRepository.selectByMap(map);
        return PageUtil.convert(configItemList, ConfigItemAssembler.INSTANCE::fromDomain);
    }

    /**
     * 获取所有配置项
     *
     * @return 配置项 DTO 列表
     */
    public List<ConfigItemDto> listAll() {
        List<ConfigItem> configItemList = configItemRepository.selectByMap(new HashMap<>());
        return ConfigItemAssembler.INSTANCE.fromDomainList(configItemList);
    }

    /**
     * 检查配置项代码是否唯一
     *
     * @param configItemId 配置项ID
     * @param code         配置项代码
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
     * @return 配置项 DTO
     */
    public ConfigItemDto getConfigItemById(Long id) {
        return ConfigItemAssembler.INSTANCE.fromDomain(configItemRepository.selectById(id));
    }

    /**
     * 新增配置项
     *
     * @param configItemDto 配置项信息 DTO
     * @param userId        操作用户ID
     * @return 结果
     */
    public int createConfigItem(ConfigItemCmd configItemCmd, String userId) {
        ConfigItem configItem = ConfigItemAssembler.INSTANCE.toDomain(configItemCmd);
        return configItemRepository.insert(configItem);
    }

    /**
     * 修改配置项
     *
     * @param configItemDto 配置项信息 DTO
     * @param userId        操作用户ID
     * @return 结果
     */
    public int modifyConfigItem(ConfigItemCmd configItemCmd, String userId) {
        ConfigItem configItem = ConfigItemAssembler.INSTANCE.toDomain(configItemCmd);
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

    // ==================== 枚举值 ====================

    /**
     * 获取配置项下所有枚举值
     *
     * @param configItemCode 配置项编码
     * @return 枚举值 DTO 列表
     */
    public List<ConfigItemOptionDto> listOption(String configItemCode) {
        Map<String, Object> map = new HashMap<>();
        map.put("configItemCode", configItemCode);
        List<ConfigItemOption> list = configItemRepository.selectOptionByMap(map);
        return ConfigItemOptionAssembler.INSTANCE.fromDomainList(list);
    }

    /**
     * 根据主键ID获取枚举值信息
     *
     * @param id 主键ID
     * @return 枚举值 DTO
     */
    public ConfigItemOptionDto getOptionById(Long id) {
        return ConfigItemOptionAssembler.INSTANCE.fromDomain(configItemRepository.selectOptionById(id));
    }

    /**
     * 新增枚举值
     *
     * @param optionDto 枚举值信息 DTO
     * @param userId    操作用户ID
     * @return 结果
     */
    public int createOption(ConfigItemOptionCmd optionCmd, String userId) {
        ConfigItemOption option = ConfigItemOptionAssembler.INSTANCE.toDomain(optionCmd);
        return configItemRepository.insertOption(option);
    }

    /**
     * 修改枚举值
     *
     * @param optionDto 枚举值信息 DTO
     * @param userId    操作用户ID
     * @return 结果
     */
    public int modifyOption(ConfigItemOptionCmd optionCmd, String userId) {
        ConfigItemOption option = ConfigItemOptionAssembler.INSTANCE.toDomain(optionCmd);
        return configItemRepository.updateOption(option);
    }

    /**
     * 批量删除枚举值
     *
     * @param ids 枚举值ID数组
     * @return 结果
     */
    public int deleteOptionByIds(Long[] ids) {
        return configItemRepository.batchPhysicalDeleteOption(ids);
    }

    // ==================== 映射 ====================

    /**
     * 获取配置项下所有映射
     *
     * @param configItemCode 配置项编码
     * @return 映射 DTO 列表
     */
    public List<ConfigItemMappingDto> listMapping(String configItemCode) {
        Map<String, Object> map = new HashMap<>();
        map.put("configItemCode", configItemCode);
        List<ConfigItemMapping> list = configItemRepository.selectMappingByMap(map);
        return ConfigItemMappingAssembler.INSTANCE.fromDomainList(list);
    }

    /**
     * 根据主键ID获取映射信息
     *
     * @param id 主键ID
     * @return 映射 DTO
     */
    public ConfigItemMappingDto getMappingById(Long id) {
        return ConfigItemMappingAssembler.INSTANCE.fromDomain(configItemRepository.selectMappingById(id));
    }

    /**
     * 新增映射
     *
     * @param mappingDto 映射信息 DTO
     * @param userId     操作用户ID
     * @return 结果
     */
    public int createMapping(ConfigItemMappingCmd mappingCmd, String userId) {
        ConfigItemMapping mapping = ConfigItemMappingAssembler.INSTANCE.toDomain(mappingCmd);
        return configItemRepository.insertMapping(mapping);
    }

    /**
     * 修改映射
     *
     * @param mappingDto 映射信息 DTO
     * @param userId     操作用户ID
     * @return 结果
     */
    public int modifyMapping(ConfigItemMappingCmd mappingCmd, String userId) {
        ConfigItemMapping mapping = ConfigItemMappingAssembler.INSTANCE.toDomain(mappingCmd);
        return configItemRepository.updateMapping(mapping);
    }

    /**
     * 批量删除映射
     *
     * @param ids 映射ID数组
     * @return 结果
     */
    public int deleteMappingByIds(Long[] ids) {
        return configItemRepository.batchPhysicalDeleteMapping(ids);
    }

}
