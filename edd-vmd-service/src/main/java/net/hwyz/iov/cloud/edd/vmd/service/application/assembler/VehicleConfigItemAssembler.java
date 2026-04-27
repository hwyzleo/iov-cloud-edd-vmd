package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleConfigItemDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleConfigItem;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VehicleConfigItemCmd;

import java.util.List;

/**
 * 车辆配置项 DTO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleConfigItemAssembler {

    VehicleConfigItemAssembler INSTANCE = Mappers.getMapper(VehicleConfigItemAssembler.class);

    /**
     * 领域对象转 DTO
     *
     * @param vehicleConfigItem 领域对象
     * @return DTO
     */
    VehicleConfigItemDto fromDomain(VehicleConfigItem vehicleConfigItem);

    /**
     * DTO 转领域对象
     *
     * @param vehicleConfigItemDto DTO
     * @return 领域对象
     */
    VehicleConfigItem toDomain(VehicleConfigItemDto vehicleConfigItemDto);
    /**
     * 命令转领域对象
     *
     * @param cmd 命令
     * @return 领域对象
     */
    VehicleConfigItem toDomain(VehicleConfigItemCmd cmd);


    /**
     * 领域对象列表转 DTO 列表
     *
     * @param vehicleConfigItemList 领域对象列表
     * @return DTO 列表
     */
    List<VehicleConfigItemDto> fromDomainList(List<VehicleConfigItem> vehicleConfigItemList);

}
