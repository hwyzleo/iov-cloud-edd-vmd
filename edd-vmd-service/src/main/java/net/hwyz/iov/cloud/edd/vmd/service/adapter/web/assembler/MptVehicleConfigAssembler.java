package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.VehicleConfigItemRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.VehicleConfigItemResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.VehicleConfigRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.VehicleConfigResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleConfigDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleConfigItemDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VehicleConfigCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VehicleConfigItemCmd;

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
    VehicleConfigResponse fromConfigDto(VehicleConfigDto vehicleConfigDto);

    /**
     * 配置 VO 转 DTO
     *
     * @param vehicleConfigVo 配置 VO
     * @return 配置 DTO
     */
    VehicleConfigDto toConfigDto(VehicleConfigRequest vehicleConfigVo);
    VehicleConfigCmd toConfigCmd(VehicleConfigRequest vo);


    /**
     * 配置 DTO 列表转 VO 列表
     *
     * @param vehicleConfigDtoList 配置 DTO 列表
     * @return 配置 VO 列表
     */
    List<VehicleConfigResponse> fromConfigDtoList(List<VehicleConfigDto> vehicleConfigDtoList);

    /**
     * 配置项 DTO 转 VO
     *
     * @param vehicleConfigItemDto 配置项 DTO
     * @return 配置项 VO
     */
    VehicleConfigItemResponse fromItemDto(VehicleConfigItemDto vehicleConfigItemDto);

    /**
     * 配置项 VO 转 DTO
     *
     * @param vehicleConfigItemVo 配置项 VO
     * @return 配置项 DTO
     */
    VehicleConfigItemDto toItemDto(VehicleConfigItemRequest vehicleConfigItemVo);
    VehicleConfigItemCmd toItemCmd(VehicleConfigItemRequest vo);


    /**
     * 配置项 DTO 列表转 VO 列表
     *
     * @param vehicleConfigItemDtoList 配置项 DTO 列表
     * @return 配置项 VO 列表
     */
    List<VehicleConfigItemResponse> fromItemDtoList(List<VehicleConfigItemDto> vehicleConfigItemDtoList);

}
