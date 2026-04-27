package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.VehicleVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.VehicleDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台车辆 VO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface MptVehicleAssembler {

    MptVehicleAssembler INSTANCE = Mappers.getMapper(MptVehicleAssembler.class);

    /**
     * DTO 转 VO
     *
     * @param vehicleDto DTO
     * @return VO
     */
    VehicleVo fromDto(VehicleDto vehicleDto);

    /**
     * DTO 列表转 VO 列表
     *
     * @param vehicleDtoList DTO 列表
     * @return VO 列表
     */
    List<VehicleVo> fromDtoList(List<VehicleDto> vehicleDtoList);

}
