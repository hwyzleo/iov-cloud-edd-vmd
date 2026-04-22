package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehModelDo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 车辆车型表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2024-09-24
 */
@Mapper
public interface VehModelDao extends BaseDao<VmdVehModelDo, Long> {

    /**
     * 通过code查询车型信息
     *
     * @param code 车型编码
     * @return 车型信息
     */
    VmdVehModelDo selectPoByCode(String code);

}
