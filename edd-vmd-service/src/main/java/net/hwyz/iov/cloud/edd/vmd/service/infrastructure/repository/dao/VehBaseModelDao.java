package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehBaseModelDo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 车辆基础车型表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-01-19
 */
@Mapper
public interface VehBaseModelDao extends BaseDao<VmdVehBaseModelDo, Long> {

    /**
     * 通过code查询基础车型信息
     *
     * @param code 基础车型编码
     * @return 基础车型信息
     */
    VmdVehBaseModelDo selectPoByCode(String code);

}
