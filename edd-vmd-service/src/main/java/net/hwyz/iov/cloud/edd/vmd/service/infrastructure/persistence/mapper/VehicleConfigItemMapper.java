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

    /**
     * 根据车架号物理删除车辆配置项
     *
     * @param vin 车架号
     * @return 影响行数
     */
    int physicalDeleteByVin(String vin);

}
