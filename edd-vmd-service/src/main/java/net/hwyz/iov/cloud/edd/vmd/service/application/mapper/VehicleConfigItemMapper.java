package net.hwyz.iov.cloud.edd.vmd.service.application.mapper;

import net.hwyz.iov.cloud.edd.vmd.api.vo.VehicleConfigItemVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehicleConfigItemDo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台车辆配置项转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleConfigItemMapper {

    VehicleConfigItemMapper INSTANCE = Mappers.getMapper(VehicleConfigItemMapper.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param vehicleConfigItemDo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    VehicleConfigItemVo fromDo(VmdVehicleConfigItemDo vehicleConfigItemDo);

    /**
     * 数据传输对象转数据对象
     *
     * @param vehicleConfigItemVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    VmdVehicleConfigItemDo toDo(VehicleConfigItemVo vehicleConfigItemVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param vehicleConfigItemDoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<VehicleConfigItemVo> fromDoList(List<VmdVehicleConfigItemDo> vehicleConfigItemDoList);

}
