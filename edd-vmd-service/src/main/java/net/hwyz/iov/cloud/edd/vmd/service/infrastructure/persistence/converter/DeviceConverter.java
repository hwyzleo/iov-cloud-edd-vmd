package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Device;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.DevicePo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 设备领域对象转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface DeviceConverter {

    DeviceConverter INSTANCE = Mappers.getMapper(DeviceConverter.class);

    /**
     * PO 转领域对象
     *
     * @param devicePo PO
     * @return 领域对象
     */
    @Mapping(source = "funcPomain", target = "funcDomain")
    @Mapping(target = "state", ignore = true)
    Device toDomain(DevicePo devicePo);

    /**
     * PO 列表转领域对象列表
     *
     * @param devicePoList PO 列表
     * @return 领域对象列表
     */
    List<Device> toDomainList(List<DevicePo> devicePoList);

    /**
     * 领域对象转 PO
     *
     * @param device 领域对象
     * @return PO
     */
    @Mapping(source = "funcDomain", target = "funcPomain")
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "rowValid", ignore = true)
    DevicePo fromDomain(Device device);
}
