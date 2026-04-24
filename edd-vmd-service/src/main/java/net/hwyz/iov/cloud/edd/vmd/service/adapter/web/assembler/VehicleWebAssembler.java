package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.VehicleResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.VehicleDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 车辆 Web 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleWebAssembler {

    VehicleWebAssembler INSTANCE = Mappers.getMapper(VehicleWebAssembler.class);

    /**
     * DTO 转响应对象
     *
     * @param vehicleDto DTO
     * @return 响应对象
     */
    VehicleResponse toResponse(VehicleDto vehicleDto);

    /**
     * DTO 列表转响应对象列表
     *
     * @param vehicleDtoList DTO 列表
     * @return 响应对象列表
     */
    List<VehicleResponse> toResponseList(List<VehicleDto> vehicleDtoList);
}
