package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.DeviceExService;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Device;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 对外服务设备信息转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface DeviceExServiceAssembler {

    DeviceExServiceAssembler INSTANCE = Mappers.getMapper(DeviceExServiceAssembler.class);

    /**
     * 领域对象转对外服务对象
     *
     * @param device 领域对象
     * @return 对外服务对象
     */
    DeviceExService fromDomain(Device device);

    /**
     * 对外服务对象转领域对象
     *
     * @param deviceExService 对外服务对象
     * @return 领域对象
     */
    Device toDomain(DeviceExService deviceExService);

    /**
     * 领域对象列表转对外服务对象列表
     *
     * @param deviceList 领域对象列表
     * @return 对外服务对象列表
     */
    List<DeviceExService> fromDomainList(List<Device> deviceList);

}
