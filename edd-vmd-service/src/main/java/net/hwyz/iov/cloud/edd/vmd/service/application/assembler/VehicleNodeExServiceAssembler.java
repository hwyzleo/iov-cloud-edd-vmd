package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VehicleNodeExResponse;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 对外服务车载节点信息转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleNodeExServiceAssembler {

    VehicleNodeExServiceAssembler INSTANCE = Mappers.getMapper(VehicleNodeExServiceAssembler.class);

    /**
     * 领域对象转对外服务对象
     *
     * @param vehicleNode 领域对象
     * @return 对外服务对象
     */
    VehicleNodeExResponse fromDomain(VehicleNode vehicleNode);

    /**
     * 对外服务对象转领域对象
     *
     * @param vehicleNodeExResponse 对外服务对象
     * @return 领域对象
     */
    VehicleNode toDomain(VehicleNodeExResponse vehicleNodeExResponse);

    /**
     * 领域对象列表转对外服务对象列表
     *
     * @param vehicleNodeList 领域对象列表
     * @return 对外服务对象列表
     */
    List<VehicleNodeExResponse> fromDomainList(List<VehicleNode> vehicleNodeList);

}
