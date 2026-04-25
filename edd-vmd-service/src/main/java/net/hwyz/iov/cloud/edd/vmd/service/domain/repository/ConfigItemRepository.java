package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigItem;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigItemMapping;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigItemOption;

import java.util.List;
import java.util.Map;

/**
 * 配置项数据仓库接口
 *
 * @author hwyz_leo
 */
public interface ConfigItemRepository {

    // ==================== 配置项 ====================

    /**
     * 根据条件查询配置项列表
     *
     * @param map 查询条件
     * @return 配置项列表
     */
    List<ConfigItem> selectByMap(Map<String, Object> map);

    /**
     * 根据主键ID查询配置项
     *
     * @param id 主键ID
     * @return 配置项
     */
    ConfigItem selectById(Long id);

    /**
     * 根据配置项代码查询配置项
     *
     * @param code 配置项代码
     * @return 配置项
     */
    ConfigItem selectByCode(String code);

    /**
     * 新增配置项
     *
     * @param configItem 配置项
     * @return 影响行数
     */
    int insert(ConfigItem configItem);

    /**
     * 修改配置项
     *
     * @param configItem 配置项
     * @return 影响行数
     */
    int update(ConfigItem configItem);

    /**
     * 批量物理删除配置项
     *
     * @param ids 主键ID数组
     * @return 影响行数
     */
    int batchPhysicalDelete(Long[] ids);

    // ==================== 配置项枚举值 ====================

    /**
     * 根据条件查询配置项枚举值列表
     *
     * @param map 查询条件
     * @return 配置项枚举值列表
     */
    List<ConfigItemOption> selectOptionByMap(Map<String, Object> map);

    /**
     * 根据主键ID查询配置项枚举值
     *
     * @param id 主键ID
     * @return 配置项枚举值
     */
    ConfigItemOption selectOptionById(Long id);

    /**
     * 根据配置项代码和枚举值代码查询配置项枚举值
     *
     * @param configItemCode 配置项代码
     * @param code           枚举值代码
     * @return 配置项枚举值
     */
    ConfigItemOption selectOptionByCode(String configItemCode, String code);

    /**
     * 新增配置项枚举值
     *
     * @param configItemOption 配置项枚举值
     * @return 影响行数
     */
    int insertOption(ConfigItemOption configItemOption);

    /**
     * 修改配置项枚举值
     *
     * @param configItemOption 配置项枚举值
     * @return 影响行数
     */
    int updateOption(ConfigItemOption configItemOption);

    /**
     * 批量物理删除配置项枚举值
     *
     * @param ids 主键ID数组
     * @return 影响行数
     */
    int batchPhysicalDeleteOption(Long[] ids);

    // ==================== 配置项映射 ====================

    /**
     * 根据条件查询配置项映射列表
     *
     * @param map 查询条件
     * @return 配置项映射列表
     */
    List<ConfigItemMapping> selectMappingByMap(Map<String, Object> map);

    /**
     * 根据主键ID查询配置项映射
     *
     * @param id 主键ID
     * @return 配置项映射
     */
    ConfigItemMapping selectMappingById(Long id);

    /**
     * 根据配置项代码、源系统、源系统代码查询配置项映射
     *
     * @param configItemCode 配置项代码
     * @param sourceSystem   源系统
     * @param sourceCode     源系统代码
     * @return 配置项映射
     */
    ConfigItemMapping selectMappingBySourceCode(String configItemCode, String sourceSystem, String sourceCode);

    /**
     * 根据配置项代码、源系统、源系统代码、源系统值查询配置项映射
     *
     * @param configItemCode 配置项代码
     * @param sourceSystem   源系统
     * @param sourceCode     源系统代码
     * @param sourceValue    源系统值
     * @return 配置项映射
     */
    ConfigItemMapping selectMappingBySourceValue(String configItemCode, String sourceSystem, String sourceCode, String sourceValue);

    /**
     * 新增配置项映射
     *
     * @param configItemMapping 配置项映射
     * @return 影响行数
     */
    int insertMapping(ConfigItemMapping configItemMapping);

    /**
     * 修改配置项映射
     *
     * @param configItemMapping 配置项映射
     * @return 影响行数
     */
    int updateMapping(ConfigItemMapping configItemMapping);

    /**
     * 批量物理删除配置项映射
     *
     * @param ids 主键ID数组
     * @return 影响行数
     */
    int batchPhysicalDeleteMapping(Long[] ids);

}
