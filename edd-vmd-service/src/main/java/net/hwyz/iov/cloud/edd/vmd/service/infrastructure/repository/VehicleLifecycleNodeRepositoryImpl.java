package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.mapper.VehicleLifecycleNodeMapper;
import net.hwyz.iov.cloud.framework.common.domain.AbstractRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.VehicleLifecycleNode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleLifecycleNodeRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.VehLifecycleDao;
import org.springframework.stereotype.Repository;

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

    private final VehLifecycleDao vehLifecycleDao;

    @Override
    public Optional<VehicleLifecycleNode> getById(String s) {
        return Optional.empty();
    }

    @Override
    public boolean save(VehicleLifecycleNode vehicleLifecycleNode) {
        switch (vehicleLifecycleNode.getState()) {
            case NEW -> vehLifecycleDao.insertPo(VehicleLifecycleNodeMapper.INSTANCE.toDo(vehicleLifecycleNode));
            case CHANGED -> vehLifecycleDao.updatePo(VehicleLifecycleNodeMapper.INSTANCE.toDo(vehicleLifecycleNode));
            default -> {
                return false;
            }
        }
        return true;
    }
}
