package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.VehicleConfigDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleConfig;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 车辆配置 DTO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleConfigAssembler {

    VehicleConfigAssembler INSTANCE = Mappers.getMapper(VehicleConfigAssembler.class);

    /**
     * 领域对象转 DTO
     *
     * @param vehicleConfig 领域对象
     * @return DTO
     */
    VehicleConfigDto fromDomain(VehicleConfig vehicleConfig);

    /**
     * DTO 转领域对象
     *
     * @param vehicleConfigDto DTO
     * @return 领域对象
     */
    VehicleConfig toDomain(VehicleConfigDto vehicleConfigDto);

    /**
     * 领域对象列表转 DTO 列表
     *
     * @param vehicleConfigList 领域对象列表
     * @return DTO 列表
     */
    List<VehicleConfigDto> fromDomainList(List<VehicleConfig> vehicleConfigList);

}
