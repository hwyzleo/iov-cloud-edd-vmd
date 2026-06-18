package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleNodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VehicleNodeCmd;

import java.util.List;

/**
 * 车载节点 DTO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleNodeAssembler {

    VehicleNodeAssembler INSTANCE = Mappers.getMapper(VehicleNodeAssembler.class);

    /**
     * 领域对象转 DTO
     *
     * @param vehicleNode 领域对象
     * @return DTO
     */
    VehicleNodeDto fromDomain(VehicleNode vehicleNode);

    /**
     * DTO 转领域对象
     *
     * @param vehicleNodeDto DTO
     * @return 领域对象
     */
    VehicleNode toDomain(VehicleNodeDto vehicleNodeDto);

    /**
     * 命令转领域对象
     *
     * @param cmd 命令
     * @return 领域对象
     */
    VehicleNode toDomain(VehicleNodeCmd cmd);

    /**
     * 领域对象列表转 DTO 列表
     *
     * @param vehicleNodeList 领域对象列表
     * @return DTO 列表
     */
    List<VehicleNodeDto> fromDomainList(List<VehicleNode> vehicleNodeList);

}
