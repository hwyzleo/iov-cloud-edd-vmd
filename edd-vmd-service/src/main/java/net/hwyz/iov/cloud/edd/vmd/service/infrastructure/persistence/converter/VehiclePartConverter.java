package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehiclePartPo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 车辆-零件绑定关系领域对象转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehiclePartConverter {

    VehiclePartConverter INSTANCE = Mappers.getMapper(VehiclePartConverter.class);

    VehiclePart toDomain(VehiclePartPo po);

    List<VehiclePart> toDomainList(List<VehiclePartPo> poList);

    VehiclePartPo fromDomain(VehiclePart domain);

    List<VehiclePartPo> fromDomainList(List<VehiclePart> domainList);
}
