package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.aggregate.Vehicle;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehBasicInfoPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 车辆领域对象转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleConverter {

    VehicleConverter INSTANCE = Mappers.getMapper(VehicleConverter.class);

    /**
     * PO 转领域对象
     *
     * @param vehBasicInfoPo PO
     * @return 领域对象
     */
    @Mapping(target = "state", ignore = true)
    Vehicle toDomain(VehBasicInfoPo vehBasicInfoPo);

    /**
     * 领域对象转 PO
     *
     * @param vehicle 领域对象
     * @return PO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "rowValid", ignore = true)
    VehBasicInfoPo fromDomain(Vehicle vehicle);
}
