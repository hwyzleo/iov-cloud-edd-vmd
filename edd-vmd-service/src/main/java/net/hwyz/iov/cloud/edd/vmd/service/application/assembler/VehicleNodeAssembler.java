package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleNodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleNode;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
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
    @Mapping(target = "nodeType", expression = "java(net.hwyz.iov.cloud.framework.common.util.StrUtil.isBlank(vehicleNode.getNodeType()) ? null : vehicleNode.getNodeType().split(\",\"))")
    @Mapping(target = "commProtocol", expression = "java(net.hwyz.iov.cloud.framework.common.util.StrUtil.isBlank(vehicleNode.getCommProtocol()) ? null : vehicleNode.getCommProtocol().split(\",\"))")
    @Mapping(target = "flashProtocol", expression = "java(net.hwyz.iov.cloud.framework.common.util.StrUtil.isBlank(vehicleNode.getFlashProtocol()) ? null : vehicleNode.getFlashProtocol().split(\",\"))")
    VehicleNodeDto fromDomain(VehicleNode vehicleNode);

    /**
     * DTO 转领域对象
     *
     * @param vehicleNodeDto DTO
     * @return 领域对象
     */
    @Mapping(target = "nodeType", expression = "java(vehicleNodeDto.getNodeType() == null ? null : String.join(\",\", vehicleNodeDto.getNodeType()))")
    @Mapping(target = "commProtocol", expression = "java(vehicleNodeDto.getCommProtocol() == null ? null : String.join(\",\", vehicleNodeDto.getCommProtocol()))")
    @Mapping(target = "flashProtocol", expression = "java(vehicleNodeDto.getFlashProtocol() == null ? null : String.join(\",\", vehicleNodeDto.getFlashProtocol()))")
    VehicleNode toDomain(VehicleNodeDto vehicleNodeDto);

    /**
     * 命令转领域对象
     *
     * @param cmd 命令
     * @return 领域对象
     */
    @Mapping(target = "nodeType", expression = "java(cmd.getNodeType() == null ? null : String.join(\",\", cmd.getNodeType()))")
    @Mapping(target = "commProtocol", expression = "java(cmd.getCommProtocol() == null ? null : String.join(\",\", cmd.getCommProtocol()))")
    @Mapping(target = "flashProtocol", expression = "java(cmd.getFlashProtocol() == null ? null : String.join(\",\", cmd.getFlashProtocol()))")
    VehicleNode toDomain(VehicleNodeCmd cmd);

    /**
     * 领域对象列表转 DTO 列表
     *
     * @param vehicleNodeList 领域对象列表
     * @return DTO 列表
     */
    List<VehicleNodeDto> fromDomainList(List<VehicleNode> vehicleNodeList);

}
