package net.hwyz.iov.cloud.edd.vmd.service.application.mapper;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.VehicleLifecycleNode;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehLifecycleDo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

/**
 * 车辆生命周期数据对象转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleLifecycleNodeMapper {

    VehicleLifecycleNodeMapper INSTANCE = Mappers.getMapper(VehicleLifecycleNodeMapper.class);

    /**
     * 数据对象转领域对象
     *
     * @param vehLifecycleDo 数据对象
     * @return 领域对象
     */
    @Mappings({})
    VehicleLifecycleNode fromDo(VmdVehLifecycleDo vehLifecycleDo);

    /**
     * 领域对象转数据对象
     *
     * @param vehicleLifecycleNode 领域对象
     * @return 数据对象
     */
    @Mappings({})
    VmdVehLifecycleDo toDo(VehicleLifecycleNode vehicleLifecycleNode);

}
