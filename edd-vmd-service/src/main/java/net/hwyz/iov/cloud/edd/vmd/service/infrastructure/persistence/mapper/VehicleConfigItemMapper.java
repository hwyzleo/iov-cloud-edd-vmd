package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehicleConfigItemPo;
import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 车辆配置项表 DAO
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-02-12
 */
@Mapper
public interface VehicleConfigItemMapper extends BaseDao<VehicleConfigItemPo, Long> {

}
