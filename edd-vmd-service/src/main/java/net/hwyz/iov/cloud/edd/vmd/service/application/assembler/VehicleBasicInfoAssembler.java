package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.aggregate.Vehicle;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehBasicInfoPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

/**
 * 车辆基础信息数据对象转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleBasicInfoAssembler {

    VehicleBasicInfoAssembler INSTANCE = Mappers.getMapper(VehicleBasicInfoAssembler.class);

    /**
     * 数据对象转领域对象
     *
     * @param vehBasicInfoPo 数据对象
     * @return 领域对象
     */
    @Mappings({})
    Vehicle toPo(VehBasicInfoPo vehBasicInfoPo);

    /**
     * 领域对象转数据对象
     *
     * @param vehiclePo 领域对象
     * @return 数据对象
     */
    @Mappings({})
    VehBasicInfoPo fromPo(Vehicle vehiclePo);

}
