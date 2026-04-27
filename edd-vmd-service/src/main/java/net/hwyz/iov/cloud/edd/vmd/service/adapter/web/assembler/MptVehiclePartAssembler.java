package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.VehiclePartRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.VehiclePartResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehiclePartDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VehiclePartCmd;

import java.util.List;

/**
 * 管理后台车辆零件 VO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface MptVehiclePartAssembler {

    MptVehiclePartAssembler INSTANCE = Mappers.getMapper(MptVehiclePartAssembler.class);

    /**
     * DTO 转 VO
     *
     * @param vehiclePartDto DTO
     * @return VO
     */
    VehiclePartResponse fromDto(VehiclePartDto vehiclePartDto);

    /**
     * VO 转 DTO
     *
     * @param vehiclePartVo VO
     * @return DTO
     */
    VehiclePartDto toDto(VehiclePartRequest vehiclePartVo);
    /**
     * VO 转命令
     *
     * @param vo VO
     * @return 命令
     */
    VehiclePartCmd toCmd(VehiclePartRequest vo);


    /**
     * DTO 列表转 VO 列表
     *
     * @param vehiclePartDtoList DTO 列表
     * @return VO 列表
     */
    List<VehiclePartResponse> fromDtoList(List<VehiclePartDto> vehiclePartDtoList);

}
