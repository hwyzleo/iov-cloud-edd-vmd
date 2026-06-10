package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.VehicleNodeRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.VehicleNodeResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleNodeDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VehicleNodeCmd;

import java.util.List;

/**
 * 管理后台车载节点 VO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface MptVehicleNodeAssembler {

    MptVehicleNodeAssembler INSTANCE = Mappers.getMapper(MptVehicleNodeAssembler.class);

    /**
     * DTO 转 VO
     *
     * @param vehicleNodeDto DTO
     * @return VO
     */
    VehicleNodeResponse fromDto(VehicleNodeDto vehicleNodeDto);

    /**
     * VO 转 DTO
     *
     * @param vehicleNodeVo VO
     * @return DTO
     */
    VehicleNodeDto toDto(VehicleNodeRequest vehicleNodeVo);

    /**
     * VO 转命令
     *
     * @param vo VO
     * @return 命令
     */
    VehicleNodeCmd toCmd(VehicleNodeRequest vo);

    /**
     * DTO 列表转 VO 列表
     *
     * @param vehicleNodeDtoList DTO 列表
     * @return VO 列表
     */
    List<VehicleNodeResponse> fromDtoList(List<VehicleNodeDto> vehicleNodeDtoList);

}
