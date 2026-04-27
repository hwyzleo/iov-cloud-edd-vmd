package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehiclePartDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VehiclePartCmd;

import java.util.List;

/**
 * 车辆零件 DTO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehiclePartAssembler {

    VehiclePartAssembler INSTANCE = Mappers.getMapper(VehiclePartAssembler.class);

    /**
     * 领域对象转 DTO
     *
     * @param vehiclePart 领域对象
     * @return DTO
     */
    VehiclePartDto fromDomain(VehiclePart vehiclePart);

    /**
     * DTO 转领域对象
     *
     * @param vehiclePartDto DTO
     * @return 领域对象
     */
    VehiclePart toDomain(VehiclePartDto vehiclePartDto);
    /**
     * 命令转领域对象
     *
     * @param cmd 命令
     * @return 领域对象
     */
    VehiclePart toDomain(VehiclePartCmd cmd);


    /**
     * 领域对象列表转 DTO 列表
     *
     * @param vehiclePartList 领域对象列表
     * @return DTO 列表
     */
    List<VehiclePartDto> fromDomainList(List<VehiclePart> vehiclePartList);

}
