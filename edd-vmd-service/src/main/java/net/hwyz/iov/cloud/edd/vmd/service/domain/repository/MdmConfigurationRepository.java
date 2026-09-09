package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Configuration;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigurationOptionCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.ConfigurationHierarchy;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;

import java.util.List;
import java.util.Map;

public interface MdmConfigurationRepository {

    List<Configuration> selectByMap(Map<String, Object> map);

    int countByMap(Map<String, Object> map);

    Configuration selectById(Long id);

    Configuration selectByCode(String code);

    int insert(Configuration configuration);

    int update(Configuration configuration);

    int batchPhysicalDelete(Long[] ids);

    List<Configuration> selectByExample(Configuration example);

    List<ConfigurationOptionCode> selectOptionCodeByExample(ConfigurationOptionCode example);

    int batchInsertOptionCode(List<ConfigurationOptionCode> optionCodeList);

    int updateOptionCode(ConfigurationOptionCode optionCode);

    int batchPhysicalDeleteOptionCode(Long[] ids);

    List<String> selectConfigurationCodeByOptionCodeMap(Map<String, String> optionCodeMap);

    Configuration selectByExternalRefId(String externalRefId);

    long countBySource(SourceType source);

    int updateById(Configuration configuration);

    /**
     * 按配置代码查询配置产品树补全视图（LEFT JOIN 降级，CR-047）
     *
     * @param code 配置编码
     * @return 配置产品树补全值对象
     */
    ConfigurationHierarchy selectHierarchyByCode(String code);

    /**
     * 批量按配置代码查询配置产品树补全视图（单次 JOIN + IN，禁止 N+1）
     *
     * @param codes 配置编码列表
     * @return 配置产品树补全值对象列表
     */
    List<ConfigurationHierarchy> selectHierarchyByCodes(List<String> codes);

    /**
     * 按条件查询配置产品树补全视图（筛选条件在数据库侧 JOIN 完成）
     *
     * @param map 查询条件
     * @return 配置产品树补全值对象列表
     */
    List<ConfigurationHierarchy> selectHierarchyByMap(Map<String, Object> map);

    /**
     * 按条件统计配置产品树补全视图数量（与 selectHierarchyByMap 同条件，支撑分页）
     *
     * @param map 查询条件
     * @return 数量
     */
    int countHierarchyByMap(Map<String, Object> map);

    /**
     * 统计有效配置中缺失 Variant 引用的条数
     *
     * @return 缺失条数
     */
    long countMissingVariant();

    /**
     * 统计有效配置中产品树任一层级投影缺失的条数
     *
     * @return 缺失条数
     */
    long countMissingHierarchy();

    /**
     * 逻辑删除配置（DELETED/DEACTIVATED 事件，不物理级联删除选项映射与车辆历史事实，CR-047）
     *
     * @param id 主键
     * @return 影响行数
     */
    int logicalDeleteById(Long id);

}
