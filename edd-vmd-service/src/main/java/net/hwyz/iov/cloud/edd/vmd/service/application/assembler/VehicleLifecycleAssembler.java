package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.VehicleLifecycleDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleLifecycle;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleLifecycleNode;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 车辆生命周期 DTO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleLifecycleAssembler {

    VehicleLifecycleAssembler INSTANCE = Mappers.getMapper(VehicleLifecycleAssembler.class);

    /**
     * 领域对象转 DTO
     *
     * @param vehicleLifecycle 领域对象
     * @return DTO
     */
    VehicleLifecycleDto fromDomain(VehicleLifecycle vehicleLifecycle);

    /**
     * DTO 转领域对象
     *
     * @param vehicleLifecycleDto DTO
     * @return 领域对象
     */
    VehicleLifecycle toDomain(VehicleLifecycleDto vehicleLifecycleDto);

    /**
     * 领域对象列表转 DTO 列表
     *
     * @param vehicleLifecycleList 领域对象列表
     * @return DTO 列表
     */
    List<VehicleLifecycleDto> fromDomainList(List<VehicleLifecycle> vehicleLifecycleList);

    /**
     * 节点列表转 DTO
     *
     * @param vin   车架号
     * @param nodes 节点列表
     * @return DTO
     */
    default VehicleLifecycleDto fromNodes(String vin, List<VehicleLifecycleNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return VehicleLifecycleDto.builder().vin(vin).build();
        }
        VehicleLifecycleNode lastNode = nodes.get(nodes.size() - 1);
        return VehicleLifecycleDto.builder()
                .vin(vin)
                .nodeCode(lastNode.getNode().name())
                .nodeTime(lastNode.getReachTime())
                .build();
    }

}
