package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.VehiclePartVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehiclePartHistoryPo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehiclePartPo;
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
public interface VehiclePartAssembler {

    VehiclePartAssembler INSTANCE = Mappers.getMapper(VehiclePartAssembler.class);

    /**
     * 数据对象转领域对象
     *
     * @param vehVehiclePartPo 数据对象
     * @return 领域对象
     */
    @Mappings({})
    VehiclePartVo fromPo(VehiclePartPo vehVehiclePartPo);

    /**
     * 领域对象转数据对象
     *
     * @param vehiclePartVo 领域对象
     * @return 数据对象
     */
    @Mappings({})
    VehiclePartPo toPo(VehiclePartVo vehiclePartVo);

    /**
     * 数据对象列表转领域对象列表
     *
     * @param vehVehiclePartPoList 数据对象列表
     * @return 领域对象列表
     */
    List<VehiclePartVo> fromPoList(List<VehiclePartPo> vehVehiclePartPoList);

    /**
     * 数据对象转历史数据对象
     *
     * @param vehiclePartPo 数据对象
     * @return 历史数据对象
     */
    @Mappings({})
    VehiclePartHistoryPo toHistory(VehiclePartPo vehiclePartPo);

    /**
     * 数据对象列表转历史数据对象列表
     *
     * @param vehiclePartPoList 数据对象列表
     * @return 历史数据对象列表
     */
    List<VehiclePartHistoryPo> toHistoryList(List<VehiclePartPo> vehiclePartPoList);

}
