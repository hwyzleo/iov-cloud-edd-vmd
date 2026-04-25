package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.aggregate.Vehicle;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

/**
 * 车辆基础信息领域对象转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleBasicInfoAssembler {

    VehicleBasicInfoAssembler INSTANCE = Mappers.getMapper(VehicleBasicInfoAssembler.class);

    /**
     * 基础信息实体转聚合根
     *
     * @param vehicleBasicInfo 基础信息实体
     * @return 聚合根
     */
    @Mappings({})
    Vehicle toAggregate(VehicleBasicInfo vehicleBasicInfo);

    /**
     * 聚合根转基础信息实体
     *
     * @param vehicle 聚合根
     * @return 基础信息实体
     */
    @Mappings({})
    VehicleBasicInfo fromAggregate(Vehicle vehicle);

}
