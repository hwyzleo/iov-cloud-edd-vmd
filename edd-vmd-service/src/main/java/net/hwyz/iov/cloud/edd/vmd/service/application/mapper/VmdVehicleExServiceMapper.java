package net.hwyz.iov.cloud.edd.vmd.service.application.mapper;

import net.hwyz.iov.cloud.edd.vmd.api.vo.VehicleExService;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehBasicInfoDo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 对外服务车辆转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface VmdVehicleExServiceMapper {

    VmdVehicleExServiceMapper INSTANCE = Mappers.getMapper(VmdVehicleExServiceMapper.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param vehBasicInfoDo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    VehicleExService fromDo(VmdVehBasicInfoDo vehBasicInfoDo);

    /**
     * 数据传输对象转数据对象
     *
     * @param vehicleExServiceDo 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    VmdVehBasicInfoDo toDo(VehicleExService vehicleExServiceDo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param vehBasicInfoDoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<VehicleExService> fromDoList(List<VmdVehBasicInfoDo> vehBasicInfoDoList);

}
