package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehBasicInfoDo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 车辆基础信息表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2024-09-24
 */
@Mapper
public interface VehBasicInfoDao extends BaseDao<VmdVehBasicInfoDo, Long> {

    /**
     * 根据车架号查询车辆基础信息
     *
     * @param vin 车架号
     * @return 车辆基础信息
     */
    VmdVehBasicInfoDo selectPoByVin(String vin);

}
