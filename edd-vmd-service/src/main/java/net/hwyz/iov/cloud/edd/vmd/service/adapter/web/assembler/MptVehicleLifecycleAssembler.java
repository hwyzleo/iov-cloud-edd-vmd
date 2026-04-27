package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.VehicleLifecycleRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.VehicleLifecycleResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleLifecycleDto;
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

}
