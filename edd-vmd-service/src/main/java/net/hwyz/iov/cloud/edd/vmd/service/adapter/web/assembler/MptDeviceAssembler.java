package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.DeviceVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.DeviceDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台设备 VO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface MptDeviceAssembler {

    MptDeviceAssembler INSTANCE = Mappers.getMapper(MptDeviceAssembler.class);

    /**
     * DTO 转 VO
     *
     * @param deviceDto DTO
     * @return VO
     */
    DeviceVo fromDto(DeviceDto deviceDto);

    /**
     * VO 转 DTO
     *
     * @param deviceVo VO
     * @return DTO
     */
    DeviceDto toDto(DeviceVo deviceVo);

    /**
     * DTO 列表转 VO 列表
     *
     * @param deviceDtoList DTO 列表
     * @return VO 列表
     */
    List<DeviceVo> fromDtoList(List<DeviceDto> deviceDtoList);

}
