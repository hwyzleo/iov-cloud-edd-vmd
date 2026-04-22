package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehFeatureCodeDo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 车辆特征值表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-02-06
 */
@Mapper
public interface VehFeatureCodeDao extends BaseDao<VmdVehFeatureCodeDo, Long> {

    /**
     * 根据车辆特征值代码获取车辆特征值信息
     *
     * @param code 车辆特征值代码
     * @return 车辆特征值信息
     */
    VmdVehFeatureCodeDo selectPoByCode(String code);

}
