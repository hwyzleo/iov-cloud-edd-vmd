package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.DeviceRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.DeviceResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.DeviceDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.DeviceCmd;

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
    DeviceResponse fromDto(DeviceDto deviceDto);

    /**
     * VO 转 DTO
     *
     * @param deviceVo VO
     * @return DTO
     */
    DeviceDto toDto(DeviceRequest deviceVo);
    /**
     * VO 转命令
     *
     * @param vo VO
     * @return 命令
     */
    DeviceCmd toCmd(DeviceRequest vo);


    /**
     * DTO 列表转 VO 列表
     *
     * @param deviceDtoList DTO 列表
     * @return VO 列表
     */
    List<DeviceResponse> fromDtoList(List<DeviceDto> deviceDtoList);

}
