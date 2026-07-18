package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.VehicleLifecycleRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.VehicleLifecycleNodeResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.VehicleLifecycleResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleLifecycleDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleLifecycleNode;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VehicleLifecycleCmd;

import java.util.List;

/**
 * 管理后台车辆生命周期 VO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface MptVehicleLifecycleAssembler {

    MptVehicleLifecycleAssembler INSTANCE = Mappers.getMapper(MptVehicleLifecycleAssembler.class);

    /**
     * 生命周期 DTO 转 VO
     *
     * @param vehicleLifecycleDto 生命周期 DTO
     * @return 生命周期 VO
     */
    VehicleLifecycleResponse fromDto(VehicleLifecycleDto vehicleLifecycleDto);

    /**
     * 生命周期 VO 转 DTO
     *
     * @param vehicleLifecycleVo 生命周期 VO
     * @return 生命周期 DTO
     */
    VehicleLifecycleDto toDto(VehicleLifecycleRequest vehicleLifecycleVo);
    /**
     * VO 转命令
     *
     * @param vo VO
     * @return 命令
     */
    VehicleLifecycleCmd toCmd(VehicleLifecycleRequest vo);


    /**
     * 生命周期 DTO 列表转 VO 列表
     *
     * @param vehicleLifecycleDtoList 生命周期 DTO 列表
     * @return 生命周期 VO 列表
     */
    List<VehicleLifecycleResponse> fromDtoList(List<VehicleLifecycleDto> vehicleLifecycleDtoList);

    /**
     * 生命周期节点实体转时间线响应
     *
     * @param node 生命周期节点实体
     * @return 时间线响应
     */
    VehicleLifecycleNodeResponse toTimelineResponse(VehicleLifecycleNode node);

    /**
     * 生命周期节点实体列表转时间线响应列表
     *
     * @param nodes 生命周期节点实体列表
     * @return 时间线响应列表
     */
    List<VehicleLifecycleNodeResponse> toTimelineResponseList(List<VehicleLifecycleNode> nodes);

}
