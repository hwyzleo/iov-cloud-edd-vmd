package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdConfigItemOptionDo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 配置项枚举值表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-02-12
 */
@Mapper
public interface ConfigItemOptionDao extends BaseDao<VmdConfigItemOptionDo, Long> {

    /**
     * 根据配置项代码和枚举值代码查询配置项枚举值信息
     *
     * @param configItemCode 配置项代码
     * @param code           枚举值代码
     * @return 配置项枚举值信息
     */
    VmdConfigItemOptionDo selectPoByCode(String configItemCode, String code);

}
