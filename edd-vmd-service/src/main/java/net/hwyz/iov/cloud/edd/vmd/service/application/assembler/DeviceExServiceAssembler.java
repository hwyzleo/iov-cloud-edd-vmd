package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.DeviceExService;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.DevicePo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
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
     * 数据对象转数据传输对象
     *
     * @param devicePo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    DeviceExService fromPo(DevicePo devicePo);

    /**
     * 数据传输对象转数据对象
     *
     * @param deviceExService 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    DevicePo toPo(DeviceExService deviceExService);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param devicePoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<DeviceExService> fromPoList(List<DevicePo> devicePoList);

}
