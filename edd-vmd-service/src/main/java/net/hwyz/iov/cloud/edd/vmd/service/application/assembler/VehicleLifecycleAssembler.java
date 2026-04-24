package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.VehicleLifecycleVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehLifecyclePo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台车辆生命周期转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleLifecycleAssembler {

    VehicleLifecycleAssembler INSTANCE = Mappers.getMapper(VehicleLifecycleAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param vehLifecyclePo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    VehicleLifecycleVo fromPo(VehLifecyclePo vehLifecyclePo);

    /**
     * 数据传输对象转数据对象
     *
     * @param vehicleLifecycleVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    VehLifecyclePo toPo(VehicleLifecycleVo vehicleLifecycleVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param vehLifecyclePoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<VehicleLifecycleVo> fromPoList(List<VehLifecyclePo> vehLifecyclePoList);

}
