package net.hwyz.iov.cloud.edd.vmd.service.application.mapper;

import net.hwyz.iov.cloud.edd.vmd.api.vo.VehiclePartVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehiclePartHistoryDo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehiclePartDo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 车辆零件数据对象转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehiclePartMapper {

    VehiclePartMapper INSTANCE = Mappers.getMapper(VehiclePartMapper.class);

    /**
     * 数据对象转领域对象
     *
     * @param vehVehiclePartDo 数据对象
     * @return 领域对象
     */
    @Mappings({})
    VehiclePartVo fromDo(VmdVehiclePartDo vehVehiclePartDo);

    /**
     * 领域对象转数据对象
     *
     * @param vehiclePartVo 领域对象
     * @return 数据对象
     */
    @Mappings({})
    VmdVehiclePartDo toDo(VehiclePartVo vehiclePartVo);

    /**
     * 数据对象列表转领域对象列表
     *
     * @param vehVehiclePartDoList 数据对象列表
     * @return 领域对象列表
     */
    List<VehiclePartVo> fromDoList(List<VmdVehiclePartDo> vehVehiclePartDoList);

    /**
     * 数据对象转历史数据对象
     *
     * @param vehiclePartDo 数据对象
     * @return 历史数据对象
     */
    @Mappings({})
    VmdVehiclePartHistoryDo toHistory(VmdVehiclePartDo vehiclePartDo);

    /**
     * 数据对象列表转历史数据对象列表
     *
     * @param vehiclePartDoList 数据对象列表
     * @return 历史数据对象列表
     */
    List<VmdVehiclePartHistoryDo> toHistoryList(List<VmdVehiclePartDo> vehiclePartDoList);

}
