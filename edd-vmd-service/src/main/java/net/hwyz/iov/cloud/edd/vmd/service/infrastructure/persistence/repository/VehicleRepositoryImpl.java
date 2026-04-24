package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.domain.AbstractRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.aggregate.Vehicle;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleLifecycleNodeRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.cache.CacheService;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.VehicleNotExistException;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.VehicleConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehBasicInfoMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehLifecycleMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehBasicInfoPo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 车辆领域仓库接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VehicleRepositoryImpl extends AbstractRepository<String, Vehicle> implements VehicleRepository {

    private final CacheService cacheService;
    private final VehBasicInfoMapper vehBasicInfoMapper;
    private final VehLifecycleMapper vehLifecycleMapper;
    private final VehicleLifecycleNodeRepository vehicleLifecycleNodeRepository;

    @Override
    public Optional<Vehicle> getById(String vin) {
        return Optional.empty();
    }

    @Override
    public boolean save(Vehicle vehicle) {
        switch (vehicle.getState()) {
            case CHANGED -> {
                vehBasicInfoMapper.updatePo(VehicleConverter.INSTANCE.fromDomain(vehicle));
                cacheService.setVehicle(vehicle);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public Vehicle getByVin(String vin) {
        Vehicle vehicle = cacheService.getVehicle(vin).orElseGet(() -> {
            log.info("从数据库加载车辆[{}]领域对象", vin);
            VehBasicInfoPo vehBasicInfoPo = vehBasicInfoMapper.selectPoByVin(vin);
            if (vehBasicInfoPo == null) {
                throw new VehicleNotExistException(vin);
            }
            Vehicle vehicleTmp = VehicleConverter.INSTANCE.toDomain(vehBasicInfoPo);
            cacheService.setVehicle(vehicleTmp);
            return vehicleTmp;
        });
        vehicle.stateLoad();
        return vehicle;
    }
}
