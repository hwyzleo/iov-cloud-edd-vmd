package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleLifecycle;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleLifecycleNode;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 车辆生命周期节点转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleLifecycleNodeAssembler {

    VehicleLifecycleNodeAssembler INSTANCE = Mappers.getMapper(VehicleLifecycleNodeAssembler.class);

    /**
     * 生命周期实体转节点实体
     *
     * @param vehicleLifecycle 生命周期实体
     * @return 节点实体
     */
    VehicleLifecycleNode toNode(VehicleLifecycle vehicleLifecycle);

    /**
     * 节点实体转生命周期实体
     *
     * @param vehicleLifecycleNode 节点实体
     * @return 生命周期实体
     */
    VehicleLifecycle fromNode(VehicleLifecycleNode vehicleLifecycleNode);

}
