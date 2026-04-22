package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdConfigItemMappingDo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 配置项映射表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-02-12
 */
@Mapper
public interface ConfigItemMappingDao extends BaseDao<VmdConfigItemMappingDo, Long> {

    /**
     * 根据配置项代码、源系统、源系统代码查询配置项映射信息
     *
     * @param configItemCode 配置项代码
     * @param sourceSystem   源系统
     * @param sourceCode     源系统代码
     * @return 配置项映射信息
     */
    VmdConfigItemMappingDo selectPoBySourceCode(String configItemCode, String sourceSystem, String sourceCode);

    /**
     * 根据配置项代码、源系统、源系统值查询配置项映射信息
     *
     * @param configItemCode 配置项代码
     * @param sourceSystem   源系统
     * @param sourceCode     源系统代码
     * @param sourceValue    源系统值
     * @return 配置项映射信息
     */
    VmdConfigItemMappingDo selectPoBySourceValue(String configItemCode, String sourceSystem, String sourceCode, String sourceValue);

}
