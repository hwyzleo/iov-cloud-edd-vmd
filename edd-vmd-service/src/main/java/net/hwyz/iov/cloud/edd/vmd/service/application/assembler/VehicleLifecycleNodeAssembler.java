package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleLifecycleNode;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehLifecyclePo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

/**
 * 车辆生命周期数据对象转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleLifecycleNodeAssembler {

    VehicleLifecycleNodeAssembler INSTANCE = Mappers.getMapper(VehicleLifecycleNodeAssembler.class);

    /**
     * 数据对象转领域对象
     *
     * @param vehLifecyclePo 数据对象
     * @return 领域对象
     */
    @Mappings({})
    VehicleLifecycleNode fromPo(VehLifecyclePo vehLifecyclePo);

    /**
     * 领域对象转数据对象
     *
     * @param vehicleLifecycleNode 领域对象
     * @return 数据对象
     */
    @Mappings({})
    VehLifecyclePo toPo(VehicleLifecycleNode vehicleLifecycleNode);

}
