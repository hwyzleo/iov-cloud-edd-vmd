package net.hwyz.iov.cloud.edd.vmd.service.application.mapper;

import net.hwyz.iov.cloud.edd.vmd.api.vo.DeviceVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdDeviceDo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台设备信息转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface DeviceMapper {

    DeviceMapper INSTANCE = Mappers.getMapper(DeviceMapper.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param deviceDo 数据对象
     * @return 数据传输对象
     */
    @Mappings({
            @Mapping(target = "commProtocol", expression = "java(net.hwyz.iov.cloud.framework.common.util.StrUtil.isBlank(deviceDo.getCommProtocol()) ? null : deviceDo.getCommProtocol().split(\",\"))"),
            @Mapping(target = "flashProtocol", expression = "java(net.hwyz.iov.cloud.framework.common.util.StrUtil.isBlank(deviceDo.getFlashProtocol()) ? null : deviceDo.getFlashProtocol().split(\",\"))"),
            @Mapping(target = "nodeType", expression = "java(net.hwyz.iov.cloud.framework.common.util.StrUtil.isBlank(deviceDo.getNodeType()) ? null :deviceDo.getNodeType().split(\",\"))")
    })
    DeviceVo fromDo(VmdDeviceDo deviceDo);

    /**
     * 数据传输对象转数据对象
     *
     * @param deviceVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({
            @Mapping(target = "commProtocol", expression = "java(deviceVo.getCommProtocol() == null ? null : java.lang.String.join(\",\", deviceVo.getCommProtocol()))"),
            @Mapping(target = "flashProtocol", expression = "java(deviceVo.getFlashProtocol() == null ? null : java.lang.String.join(\",\", deviceVo.getFlashProtocol()))"),
            @Mapping(target = "nodeType", expression = "java(deviceVo.getNodeType() == null ? null : java.lang.String.join(\",\", deviceVo.getNodeType()))")
    })
    VmdDeviceDo toDo(DeviceVo deviceVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param deviceDoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<DeviceVo> fromDoList(List<VmdDeviceDo> deviceDoList);

}
