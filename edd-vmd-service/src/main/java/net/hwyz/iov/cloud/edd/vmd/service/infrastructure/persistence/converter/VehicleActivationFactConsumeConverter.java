package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleActivationFactConsume;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehicleActivationFactConsumePo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 车辆首次激活候选事实消费台账 领域对象转换器（VMD-DSN-CR-077）
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleActivationFactConsumeConverter {

    VehicleActivationFactConsumeConverter INSTANCE =
            Mappers.getMapper(VehicleActivationFactConsumeConverter.class);

    VehicleActivationFactConsume toDomain(VehicleActivationFactConsumePo po);

    List<VehicleActivationFactConsume> toDomainList(List<VehicleActivationFactConsumePo> poList);

    VehicleActivationFactConsumePo fromDomain(VehicleActivationFactConsume domain);

    List<VehicleActivationFactConsumePo> fromDomainList(List<VehicleActivationFactConsume> domainList);
}
