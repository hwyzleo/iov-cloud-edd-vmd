package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehDetailInfoDo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 * 车辆详细信息表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2025-05-07
 */
@Mapper
public interface VehDetailInfoDao extends BaseDao<VmdVehDetailInfoDo, Long> {

    /**
     * 根据车架号查询车辆详细信息
     *
     * @param vin 车架号
     * @return 车辆详细信息
     */
    List<VmdVehDetailInfoDo> selectPoByVin(String vin);

}
