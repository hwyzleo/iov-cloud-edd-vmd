package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.cache;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.aggregate.Vehicle;

import java.util.Optional;

/**
 * 缓存服务接口
 *
 * @author hwyz_leo
 */
public interface CacheService {

    /**
     * 获取车辆领域对象缓存
     *
     * @param vin 车架号
     * @return 车辆领域对象
     */
    Optional<Vehicle> getVehicle(String vin);

    /**
     * 设置车辆领域对象缓存
     *
     * @param vehicle 车辆领域对象
     */
    void setVehicle(Vehicle vehicle);

}
