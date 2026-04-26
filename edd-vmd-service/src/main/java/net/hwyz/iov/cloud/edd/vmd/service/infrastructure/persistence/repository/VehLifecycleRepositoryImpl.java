package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleLifecycle;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehLifecycleRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.VehicleLifecycleConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehLifecycleMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 车辆生命周期数据仓库接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VehLifecycleRepositoryImpl implements VehLifecycleRepository {

    private final VehLifecycleMapper vehLifecycleMapper;

    @Override
    public List<VehicleLifecycle> selectByMap(Map<String, Object> map) {
        return VehicleLifecycleConverter.INSTANCE.toDomainList(vehLifecycleMapper.selectPoByMap(map));
    }

    @Override
    public VehicleLifecycle selectById(Long id) {
        return VehicleLifecycleConverter.INSTANCE.toDomain(vehLifecycleMapper.selectPoById(id));
    }

    @Override
    public List<VehicleLifecycle> selectByVin(String vin) {
        return VehicleLifecycleConverter.INSTANCE.toDomainList(vehLifecycleMapper.selectPoByMap(Map.of("vin", vin)));
    }

    @Override
    public int insert(VehicleLifecycle vehicleLifecycle) {
        return vehLifecycleMapper.insertPo(VehicleLifecycleConverter.INSTANCE.fromDomain(vehicleLifecycle));
    }

    @Override
    public int update(VehicleLifecycle vehicleLifecycle) {
        return vehLifecycleMapper.updatePo(VehicleLifecycleConverter.INSTANCE.fromDomain(vehicleLifecycle));
    }

    @Override
    public int batchPhysicalDelete(Long[] ids) {
        return vehLifecycleMapper.batchPhysicalDeletePo(ids);
    }

    @Override
    public int physicalDeleteByVin(String vin) {
        return vehLifecycleMapper.batchPhysicalDeletePoByVin(vin);
    }

}
