package net.hwyz.iov.cloud.edd.vmd.service.application.mapper;

import net.hwyz.iov.cloud.edd.vmd.api.vo.ManufacturerVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehManufacturerDo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台车辆工厂转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface ManufacturerMapper {

    ManufacturerMapper INSTANCE = Mappers.getMapper(ManufacturerMapper.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param vehManufacturerDo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    ManufacturerVo fromDo(VmdVehManufacturerDo vehManufacturerDo);

    /**
     * 数据传输对象转数据对象
     *
     * @param manufacturerVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    VmdVehManufacturerDo toDo(ManufacturerVo manufacturerVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param vehManufacturerDoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<ManufacturerVo> fromDoList(List<VmdVehManufacturerDo> vehManufacturerDoList);

}
