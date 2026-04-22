package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehiclePartHistoryDo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 车辆零件变更历史表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-01-27
 */
@Mapper
public interface VehiclePartHistoryDao extends BaseDao<VmdVehiclePartHistoryDo, Long> {

}
