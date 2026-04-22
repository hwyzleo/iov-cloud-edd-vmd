package net.hwyz.iov.cloud.edd.vmd.service.application.mapper;

import net.hwyz.iov.cloud.edd.vmd.api.vo.VehicleLifecycleVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehLifecycleDo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台车辆生命周期转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleLifecycleMapper {

    VehicleLifecycleMapper INSTANCE = Mappers.getMapper(VehicleLifecycleMapper.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param vehLifecycleDo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    VehicleLifecycleVo fromDo(VmdVehLifecycleDo vehLifecycleDo);

    /**
     * 数据传输对象转数据对象
     *
     * @param vehicleLifecycleVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    VmdVehLifecycleDo toDo(VehicleLifecycleVo vehicleLifecycleVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param vehLifecycleDoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<VehicleLifecycleVo> fromDoList(List<VmdVehLifecycleDo> vehLifecycleDoList);

}
