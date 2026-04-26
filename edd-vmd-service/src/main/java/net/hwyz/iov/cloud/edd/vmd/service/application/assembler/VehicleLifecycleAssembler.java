package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.VehicleLifecycleVo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleLifecycle;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleLifecycleNode;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 车辆生命周期转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleLifecycleAssembler {

    VehicleLifecycleAssembler INSTANCE = Mappers.getMapper(VehicleLifecycleAssembler.class);

    /**
     * 将节点列表转换为领域对象
     *
     * @param vin   车架号
     * @param nodes 节点列表
     * @return 领域对象
     */
    default VehicleLifecycle fromNodes(String vin, List<VehicleLifecycleNode> nodes) {
        VehicleLifecycle vehicleLifecycle = VehicleLifecycle.builder()
                .vin(vin)
                .build();
        return vehicleLifecycle;
    }

    /**
     * 领域对象转数据传输对象
     *
     * @param vehicleLifecycle 领域对象
     * @return 数据传输对象
     */
    VehicleLifecycleVo fromDomain(VehicleLifecycle vehicleLifecycle);

    /**
     * 数据传输对象转领域对象
     *
     * @param vehicleLifecycleVo 数据传输对象
     * @return 领域对象
     */
    VehicleLifecycle toDomain(VehicleLifecycleVo vehicleLifecycleVo);

    /**
     * 领域对象列表转数据传输对象列表
     *
     * @param vehicleLifecycleList 领域对象列表
     * @return 数据传输对象列表
     */
    List<VehicleLifecycleVo> fromDomainList(List<VehicleLifecycle> vehicleLifecycleList);

}
