package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdSupplierDo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 供应商表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-01-23
 */
@Mapper
public interface SupplierDao extends BaseDao<VmdSupplierDo, Long> {

    /**
     * 根据供应商代码查询供应商信息
     *
     * @param code 供应商代码
     * @return 供应商信息
     */
    VmdSupplierDo selectPoByCode(String code);

}
