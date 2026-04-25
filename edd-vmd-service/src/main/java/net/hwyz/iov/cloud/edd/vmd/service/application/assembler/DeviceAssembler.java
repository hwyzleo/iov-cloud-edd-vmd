package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.DeviceVo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Device;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台设备转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface DeviceAssembler {

    DeviceAssembler INSTANCE = Mappers.getMapper(DeviceAssembler.class);

    /**
     * 领域对象转数据传输对象
     *
     * @param device 领域对象
     * @return 数据传输对象
     */
    @Mappings({
            @Mapping(target = "nodeType", expression = "java(net.hwyz.iov.cloud.framework.common.util.StrUtil.isBlank(device.getNodeType()) ? null : device.getNodeType().split(\",\"))"),
            @Mapping(target = "commProtocol", expression = "java(net.hwyz.iov.cloud.framework.common.util.StrUtil.isBlank(device.getCommProtocol()) ? null : device.getCommProtocol().split(\",\"))"),
            @Mapping(target = "flashProtocol", expression = "java(net.hwyz.iov.cloud.framework.common.util.StrUtil.isBlank(device.getFlashProtocol()) ? null : device.getFlashProtocol().split(\",\"))")
    })
    DeviceVo fromDomain(Device device);

    /**
     * 数据传输对象转领域对象
     *
     * @param deviceVo 数据传输对象
     * @return 领域对象
     */
    @Mappings({
            @Mapping(target = "nodeType", expression = "java(deviceVo.getNodeType() == null ? null : String.join(\",\", deviceVo.getNodeType()))"),
            @Mapping(target = "commProtocol", expression = "java(deviceVo.getCommProtocol() == null ? null : String.join(\",\", deviceVo.getCommProtocol()))"),
            @Mapping(target = "flashProtocol", expression = "java(deviceVo.getFlashProtocol() == null ? null : String.join(\",\", deviceVo.getFlashProtocol()))")
    })
    Device toDomain(DeviceVo deviceVo);

    /**
     * 领域对象列表转数据传输对象列表
     *
     * @param deviceList 领域对象列表
     * @return 数据传输对象列表
     */
    List<DeviceVo> fromDomainList(List<Device> deviceList);

}
