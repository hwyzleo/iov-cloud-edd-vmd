package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.VehicleConfigItemVo;
import net.hwyz.iov.cloud.edd.vmd.api.vo.VehicleConfigVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.VehicleConfigDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.VehicleConfigItemDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台车辆配置 VO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface MptVehicleConfigAssembler {

    MptVehicleConfigAssembler INSTANCE = Mappers.getMapper(MptVehicleConfigAssembler.class);

    /**
     * 配置 DTO 转 VO
     *
     * @param vehicleConfigDto 配置 DTO
     * @return 配置 VO
     */
    VehicleConfigVo fromConfigDto(VehicleConfigDto vehicleConfigDto);

    /**
     * 配置 VO 转 DTO
     *
     * @param vehicleConfigVo 配置 VO
     * @return 配置 DTO
     */
    VehicleConfigDto toConfigDto(VehicleConfigVo vehicleConfigVo);

    /**
     * 配置 DTO 列表转 VO 列表
     *
     * @param vehicleConfigDtoList 配置 DTO 列表
     * @return 配置 VO 列表
     */
    List<VehicleConfigVo> fromConfigDtoList(List<VehicleConfigDto> vehicleConfigDtoList);

    /**
     * 配置项 DTO 转 VO
     *
     * @param vehicleConfigItemDto 配置项 DTO
     * @return 配置项 VO
     */
    VehicleConfigItemVo fromItemDto(VehicleConfigItemDto vehicleConfigItemDto);

    /**
     * 配置项 VO 转 DTO
     *
     * @param vehicleConfigItemVo 配置项 VO
     * @return 配置项 DTO
     */
    VehicleConfigItemDto toItemDto(VehicleConfigItemVo vehicleConfigItemVo);

    /**
     * 配置项 DTO 列表转 VO 列表
     *
     * @param vehicleConfigItemDtoList 配置项 DTO 列表
     * @return 配置项 VO 列表
     */
    List<VehicleConfigItemVo> fromItemDtoList(List<VehicleConfigItemDto> vehicleConfigItemDtoList);

}
