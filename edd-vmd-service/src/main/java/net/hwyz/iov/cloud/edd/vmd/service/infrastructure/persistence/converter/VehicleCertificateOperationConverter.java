package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleCertificateOperation;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehicleCertificateOperationPo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 证书人工补偿操作审计领域对象转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleCertificateOperationConverter {

    VehicleCertificateOperationConverter INSTANCE = Mappers.getMapper(VehicleCertificateOperationConverter.class);

    VehicleCertificateOperation toDomain(VehicleCertificateOperationPo po);

    List<VehicleCertificateOperation> toDomainList(List<VehicleCertificateOperationPo> poList);

    VehicleCertificateOperationPo fromDomain(VehicleCertificateOperation domain);

    List<VehicleCertificateOperationPo> fromDomainList(List<VehicleCertificateOperation> domainList);

}
