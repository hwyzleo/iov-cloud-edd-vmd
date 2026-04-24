package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.ManufacturerVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehManufacturerPo;
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
public interface ManufacturerAssembler {

    ManufacturerAssembler INSTANCE = Mappers.getMapper(ManufacturerAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param vehManufacturerPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    ManufacturerVo fromPo(VehManufacturerPo vehManufacturerPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param manufacturerVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    VehManufacturerPo toPo(ManufacturerVo manufacturerVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param vehManufacturerPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<ManufacturerVo> fromPoList(List<VehManufacturerPo> vehManufacturerPoList);

}
