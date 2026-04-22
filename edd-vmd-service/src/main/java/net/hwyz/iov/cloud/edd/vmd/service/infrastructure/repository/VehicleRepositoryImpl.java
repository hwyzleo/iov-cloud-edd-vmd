package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.domain.AbstractRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.Vehicle;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleLifecycleNodeRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.cache.CacheService;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.exception.VehicleNotExistException;
import net.hwyz.iov.cloud.edd.vmd.service.application.mapper.*;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.VehBasicInfoDao;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.VehLifecycleDao;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehBasicInfoDo;
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
    private final VehBasicInfoDao vehBasicInfoDao;
    private final VehLifecycleDao vehLifecycleDao;
    private final VehicleLifecycleNodeRepository vehicleLifecycleNodeRepository;

    @Override
    public Optional<Vehicle> getById(String vin) {
        return Optional.empty();
    }

    @Override
    public boolean save(Vehicle vehicleDo) {
        switch (vehicleDo.getState()) {
            case CHANGED -> {
                vehBasicInfoDao.updatePo(VehicleBasicInfoMapper.INSTANCE.fromDo(vehicleDo));
                cacheService.setVehicle(vehicleDo);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public Vehicle getByVin(String vin) {
        Vehicle vehicleDo = cacheService.getVehicle(vin).orElseGet(() -> {
            log.info("从数据库加载车辆[{}]领域对象", vin);
            VmdVehBasicInfoDo vehBasicInfoPo = vehBasicInfoDao.selectPoByVin(vin);
            if (vehBasicInfoPo == null) {
                throw new VehicleNotExistException(vin);
            }
            Vehicle vehicleDoTmp = Vehicle.builder()
                    .vin(vin)
                    .eolTime(vehBasicInfoPo.getEolTime())
                    .orderNum(vehBasicInfoPo.getOrderNum())
                    .build();
            cacheService.setVehicle(vehicleDoTmp);
            return vehicleDoTmp;
        });
        vehicleDo.stateLoad();
        return vehicleDo;
    }
}
