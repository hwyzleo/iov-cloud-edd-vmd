package net.hwyz.iov.cloud.edd.vmd.service.application.mapper;

import net.hwyz.iov.cloud.edd.vmd.api.vo.VehicleConfigVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehicleConfigDo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台车辆配置转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface VmdVehicleConfigMapper {

    VmdVehicleConfigMapper INSTANCE = Mappers.getMapper(VmdVehicleConfigMapper.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param vehicleConfigDo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    VehicleConfigVo fromDo(VmdVehicleConfigDo vehicleConfigDo);

    /**
     * 数据传输对象转数据对象
     *
     * @param vehicleConfigVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    VmdVehicleConfigDo toDo(VehicleConfigVo vehicleConfigVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param vehicleConfigDoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<VehicleConfigVo> fromDoList(List<VmdVehicleConfigDo> vehicleConfigDoList);

}
