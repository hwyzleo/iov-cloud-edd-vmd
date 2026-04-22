package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehFeatureFamilyDo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 车辆特征族表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-02-06
 */
@Mapper
public interface VehFeatureFamilyDao extends BaseDao<VmdVehFeatureFamilyDo, Long> {

    /**
     * 根据车辆特征族代码获取车辆特征族信息
     *
     * @param code 车辆特征族代码
     * @return 车辆特征族信息
     */
    VmdVehFeatureFamilyDo selectPoByCode(String code);

}
