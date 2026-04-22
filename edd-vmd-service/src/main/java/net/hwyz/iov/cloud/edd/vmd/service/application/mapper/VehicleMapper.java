package net.hwyz.iov.cloud.edd.vmd.service.application.mapper;

import net.hwyz.iov.cloud.edd.vmd.api.vo.VehicleVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehBasicInfoDo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台车辆转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleMapper {

    VehicleMapper INSTANCE = Mappers.getMapper(VehicleMapper.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param vehBasicInfoDo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    VehicleVo fromDo(VmdVehBasicInfoDo vehBasicInfoDo);

    /**
     * 数据传输对象转数据对象
     *
     * @param vehicleVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    VmdVehBasicInfoDo toDo(VehicleVo vehicleVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param vehBasicInfoDoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<VehicleVo> fromDoList(List<VmdVehBasicInfoDo> vehBasicInfoDoList);

}
