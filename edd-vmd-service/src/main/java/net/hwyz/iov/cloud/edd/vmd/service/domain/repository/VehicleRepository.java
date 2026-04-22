package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.framework.common.domain.BaseRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.Vehicle;

/**
 * 车辆领域仓库接口
 *
 * @author hwyz_leo
 */
public interface VehicleRepository extends BaseRepository<String, Vehicle> {

    /**
     * 根据车车架号获取车辆领域对象
     *
     * @param vin 车架号
     * @return 车辆领域对象
     */
    Vehicle getByVin(String vin);

}
