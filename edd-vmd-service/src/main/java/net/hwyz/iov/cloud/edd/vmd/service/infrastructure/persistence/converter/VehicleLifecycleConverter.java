package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleLifecycle;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehLifecyclePo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 车辆生命周期领域对象转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleLifecycleConverter {

    VehicleLifecycleConverter INSTANCE = Mappers.getMapper(VehicleLifecycleConverter.class);

    /**
     * PO 转领域对象
     *
     * @param vehLifecyclePo PO
     * @return 领域对象
     */
    @Mapping(target = "state", ignore = true)
    VehicleLifecycle toDomain(VehLifecyclePo vehLifecyclePo);

    /**
     * PO 列表转领域对象列表
     *
     * @param vehLifecyclePoList PO 列表
     * @return 领域对象列表
     */
    List<VehicleLifecycle> toDomainList(List<VehLifecyclePo> vehLifecyclePoList);

    /**
     * 领域对象转 PO
     *
     * @param vehicleLifecycle 领域对象
     * @return PO
     */
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "rowValid", ignore = true)
    VehLifecyclePo fromDomain(VehicleLifecycle vehicleLifecycle);
}
