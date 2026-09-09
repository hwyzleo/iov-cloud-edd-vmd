package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmConfigurationHierarchyPo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmConfigurationPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 车辆生产配置表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2024-10-11
 */
@Mapper
public interface MdmConfigurationMapper extends BaseDao<MdmConfigurationPo, Long> {

    /**
     * 通过code查询生产配置信息
     *
     * @param code 生产配置编码
     * @return 生产配置信息
     */
    MdmConfigurationPo selectPoByCode(String code);

    /**
     * 通过外部引用ID查询生产配置信息
     *
     * @param externalRefId 外部引用ID
     * @return 生产配置信息
     */
    MdmConfigurationPo selectPoByExternalRefId(@Param("externalRefId") String externalRefId);

    /**
     * 统计指定来源的生产配置数量
     *
     * @param source 数据来源
     * @return 数量
     */
    long countPoBySource(@Param("source") String source);

    /**
     * 按配置代码查询配置产品树补全视图（LEFT JOIN，上层投影缺失时派生字段为 null）
     *
     * @param code 配置编码
     * @return 配置产品树补全视图
     */
    MdmConfigurationHierarchyPo selectConfigurationHierarchyByCode(@Param("code") String code);

    /**
     * 批量按配置代码查询配置产品树补全视图（禁止 N+1，单次 JOIN + IN）
     *
     * @param codes 配置编码列表
     * @return 配置产品树补全视图列表
     */
    List<MdmConfigurationHierarchyPo> selectConfigurationHierarchyByCodes(@Param("codes") List<String> codes);

    /**
     * 按条件查询配置产品树补全视图（筛选条件在数据库侧 JOIN 完成，支持分页）
     *
     * @param map 查询条件（code/name/variantCode/modelCode/carLineCode/platformCode/brandCode/beginTime/endTime）
     * @return 配置产品树补全视图列表
     */
    List<MdmConfigurationHierarchyPo> selectConfigurationHierarchyByMap(Map<String, Object> map);

    /**
     * 按条件统计配置产品树补全视图数量（与 selectConfigurationHierarchyByMap 同条件，支撑分页）
     *
     * @param map 查询条件
     * @return 数量
     */
    int countConfigurationHierarchyByMap(Map<String, Object> map);

    /**
     * 统计有效配置中 variant_code 无法在 tb_mdm_variant 追溯的条数（ProjectionIntegrityChecker）
     *
     * @return 缺失 Variant 引用条数
     */
    long countPoMissingVariant();

    /**
     * 统计有效配置中产品树（Variant/Model/CarLine/Platform/Brand）任一层级投影缺失的条数（ProjectionIntegrityChecker）
     *
     * @return 缺失层级引用条数
     */
    long countPoMissingHierarchy();

}
