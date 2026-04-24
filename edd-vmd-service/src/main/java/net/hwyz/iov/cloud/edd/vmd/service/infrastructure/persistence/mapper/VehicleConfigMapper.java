package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehicleConfigPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 车辆配置表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-02-12
 */
@Mapper
public interface VehicleConfigMapper extends BaseDao<VehicleConfigPo, Long> {

}
