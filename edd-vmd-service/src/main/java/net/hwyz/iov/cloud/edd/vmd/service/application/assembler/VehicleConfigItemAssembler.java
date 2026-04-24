package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.VehicleConfigItemVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehicleConfigItemPo;
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
public interface VehicleConfigItemAssembler {

    VehicleConfigItemAssembler INSTANCE = Mappers.getMapper(VehicleConfigItemAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param vehicleConfigItemPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    VehicleConfigItemVo fromPo(VehicleConfigItemPo vehicleConfigItemPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param vehicleConfigItemVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    VehicleConfigItemPo toPo(VehicleConfigItemVo vehicleConfigItemVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param vehicleConfigItemPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<VehicleConfigItemVo> fromPoList(List<VehicleConfigItemPo> vehicleConfigItemPoList);

}
