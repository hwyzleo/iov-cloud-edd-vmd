package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.VehicleExService;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehBasicInfoPo;
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
public interface VmdVehicleExServiceAssembler {

    VmdVehicleExServiceAssembler INSTANCE = Mappers.getMapper(VmdVehicleExServiceAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param vehBasicInfoPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    VehicleExService fromPo(VehBasicInfoPo vehBasicInfoPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param vehicleExServicePo 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    VehBasicInfoPo toPo(VehicleExService vehicleExServicePo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param vehBasicInfoPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<VehicleExService> fromPoList(List<VehBasicInfoPo> vehBasicInfoPoList);

}
