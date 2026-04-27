package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.DeviceDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Device;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 设备 DTO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface DeviceAssembler {

    DeviceAssembler INSTANCE = Mappers.getMapper(DeviceAssembler.class);

    /**
     * 领域对象转 DTO
     *
     * @param device 领域对象
     * @return DTO
     */
    @Mapping(target = "nodeType", expression = "java(net.hwyz.iov.cloud.framework.common.util.StrUtil.isBlank(device.getNodeType()) ? null : device.getNodeType().split(\",\"))")
    @Mapping(target = "commProtocol", expression = "java(net.hwyz.iov.cloud.framework.common.util.StrUtil.isBlank(device.getCommProtocol()) ? null : device.getCommProtocol().split(\",\"))")
    @Mapping(target = "flashProtocol", expression = "java(net.hwyz.iov.cloud.framework.common.util.StrUtil.isBlank(device.getFlashProtocol()) ? null : device.getFlashProtocol().split(\",\"))")
    DeviceDto fromDomain(Device device);

    /**
     * DTO 转领域对象
     *
     * @param deviceDto DTO
     * @return 领域对象
     */
    @Mapping(target = "nodeType", expression = "java(deviceDto.getNodeType() == null ? null : String.join(\",\", deviceDto.getNodeType()))")
    @Mapping(target = "commProtocol", expression = "java(deviceDto.getCommProtocol() == null ? null : String.join(\",\", deviceDto.getCommProtocol()))")
    @Mapping(target = "flashProtocol", expression = "java(deviceDto.getFlashProtocol() == null ? null : String.join(\",\", deviceDto.getFlashProtocol()))")
    Device toDomain(DeviceDto deviceDto);

    /**
     * 领域对象列表转 DTO 列表
     *
     * @param deviceList 领域对象列表
     * @return DTO 列表
     */
    List<DeviceDto> fromDomainList(List<Device> deviceList);

}
