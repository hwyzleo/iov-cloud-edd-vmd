package net.hwyz.iov.cloud.edd.vmd.service.application.mapper;

import net.hwyz.iov.cloud.edd.vmd.api.vo.VehicleImportDataVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehImportDataDo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台车辆导入数据转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface VmdVehicleImportDataMapper {

    VmdVehicleImportDataMapper INSTANCE = Mappers.getMapper(VmdVehicleImportDataMapper.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param vehicleImportDataDo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    VehicleImportDataVo fromDo(VmdVehImportDataDo vehicleImportDataDo);

    /**
     * 数据传输对象转数据对象
     *
     * @param vehicleImportDataVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    VmdVehImportDataDo toDo(VehicleImportDataVo vehicleImportDataVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param vehicleImportDataDoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<VehicleImportDataVo> fromDoList(List<VmdVehImportDataDo> vehicleImportDataDoList);

}
