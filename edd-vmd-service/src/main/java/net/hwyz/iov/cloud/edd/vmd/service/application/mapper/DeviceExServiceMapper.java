package net.hwyz.iov.cloud.edd.vmd.service.application.mapper;

import net.hwyz.iov.cloud.edd.vmd.api.vo.DeviceExService;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdDeviceDo;
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
public interface DeviceExServiceMapper {

    DeviceExServiceMapper INSTANCE = Mappers.getMapper(DeviceExServiceMapper.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param deviceDo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    DeviceExService fromDo(VmdDeviceDo deviceDo);

    /**
     * 数据传输对象转数据对象
     *
     * @param deviceExService 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    VmdDeviceDo toDo(DeviceExService deviceExService);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param deviceDoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<DeviceExService> fromDoList(List<VmdDeviceDo> deviceDoList);

}
