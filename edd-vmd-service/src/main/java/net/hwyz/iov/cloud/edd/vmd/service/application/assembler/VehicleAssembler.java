package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.aggregate.Vehicle;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VehicleCmd;

import java.util.List;

/**
 * 车辆 DTO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleAssembler {

    VehicleAssembler INSTANCE = Mappers.getMapper(VehicleAssembler.class);

    /**
     * 领域对象转 DTO
     *
     * @param vehicle 领域对象
     * @return DTO
     */
    VehicleDto toDto(Vehicle vehicle);

    /**
     * 基础信息领域对象转 DTO
     *
     * @param vehicleBasicInfo 基础信息领域对象
     * @return DTO
     */
    VehicleDto fromBasicInfo(VehicleBasicInfo vehicleBasicInfo);

    /**
     * 基础信息领域对象列表转 DTO 列表
     *
     * @param vehicleBasicInfoList 基础信息领域对象列表
     * @return DTO 列表
     */
    List<VehicleDto> fromBasicInfoList(List<VehicleBasicInfo> vehicleBasicInfoList);

    /**
     * 领域对象列表转 DTO 列表
     *
     * @param vehicleList 领域对象列表
     * @return DTO 列表
     */
    List<VehicleDto> toDtoList(List<Vehicle> vehicleList);

    /**
     * DTO 转领域对象
     *
     * @param vehicleDto DTO
     * @return 领域对象
     */
    Vehicle toDomain(VehicleDto vehicleDto);
    /**
     * 命令转领域对象
     *
     * @param cmd 命令
     * @return 领域对象
     */
    Vehicle toDomain(VehicleCmd cmd);

}
