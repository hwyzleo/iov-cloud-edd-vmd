package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.VehiclePartVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.VehiclePartDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

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
    VehiclePartVo fromDto(VehiclePartDto vehiclePartDto);

    /**
     * VO 转 DTO
     *
     * @param vehiclePartVo VO
     * @return DTO
     */
    VehiclePartDto toDto(VehiclePartVo vehiclePartVo);

    /**
     * DTO 列表转 VO 列表
     *
     * @param vehiclePartDtoList DTO 列表
     * @return VO 列表
     */
    List<VehiclePartVo> fromDtoList(List<VehiclePartDto> vehiclePartDtoList);

}
