package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleLifecycleNode;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehLifecyclePo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 车辆生命周期节点领域对象转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleLifecycleNodeConverter {

    VehicleLifecycleNodeConverter INSTANCE = Mappers.getMapper(VehicleLifecycleNodeConverter.class);

    /**
     * PO 转领域对象
     *
     * @param vehLifecyclePo PO
     * @return 领域对象
     */
    VehicleLifecycleNode toDomain(VehLifecyclePo vehLifecyclePo);

    /**
     * 领域对象转 PO
     *
     * @param vehicleLifecycleNode 领域对象
     * @return PO
     */
    VehLifecyclePo fromDomain(VehicleLifecycleNode vehicleLifecycleNode);
}
