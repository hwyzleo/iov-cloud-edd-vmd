package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleLifecycleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleLifecycleNodeRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.VehicleLifecycleNodeConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehLifecycleMapper;
import net.hwyz.iov.cloud.framework.common.domain.AbstractRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 车辆生命周期节点领域仓库接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VehicleLifecycleNodeRepositoryImpl extends AbstractRepository<String, VehicleLifecycleNode> implements VehicleLifecycleNodeRepository {

    private final VehLifecycleMapper vehLifecycleMapper;

    @Override
    public List<VehicleLifecycleNode> selectByVin(String vin) {
        return vehLifecycleMapper.selectPoByMap(Map.of("vin", vin)).stream()
                .map(VehicleLifecycleNodeConverter.INSTANCE::toDomain)
                .toList();
    }

    @Override
    public Optional<VehicleLifecycleNode> getById(String s) {
        return Optional.empty();
    }

    @Override
    public boolean save(VehicleLifecycleNode vehicleLifecycleNode) {
        switch (vehicleLifecycleNode.getState()) {
            case NEW -> vehLifecycleMapper.insertPo(VehicleLifecycleNodeConverter.INSTANCE.fromDomain(vehicleLifecycleNode));
            case CHANGED -> vehLifecycleMapper.updatePo(VehicleLifecycleNodeConverter.INSTANCE.fromDomain(vehicleLifecycleNode));
            default -> {
                return false;
            }
        }
        return true;
    }
}
