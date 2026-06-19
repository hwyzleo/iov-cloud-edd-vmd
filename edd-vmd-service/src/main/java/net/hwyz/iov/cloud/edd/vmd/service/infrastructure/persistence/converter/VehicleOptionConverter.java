package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleOption;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehicleOptionPo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 单车选项值快照领域对象转换器
 *
 * @author VMD-DSN-CR-030 / US-043
 */
@Mapper
public interface VehicleOptionConverter {

    VehicleOptionConverter INSTANCE = Mappers.getMapper(VehicleOptionConverter.class);

    VehicleOption toDomain(VehicleOptionPo po);

    List<VehicleOption> toDomainList(List<VehicleOptionPo> poList);

    VehicleOptionPo fromDomain(VehicleOption domain);

    List<VehicleOptionPo> fromDomainList(List<VehicleOption> domainList);
}
