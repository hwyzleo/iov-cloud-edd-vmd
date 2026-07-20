package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleCertificate;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehicleCertificatePo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 车辆设备证书领域对象转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleCertificateConverter {

    VehicleCertificateConverter INSTANCE = Mappers.getMapper(VehicleCertificateConverter.class);

    @Mapping(source = "certStatus", target = "certStatus", qualifiedByName = "stringToCertificateStatus")
    VehicleCertificate toDomain(VehicleCertificatePo po);

    List<VehicleCertificate> toDomainList(List<VehicleCertificatePo> poList);

    @Mapping(source = "certStatus", target = "certStatus", qualifiedByName = "certificateStatusToString")
    VehicleCertificatePo fromDomain(VehicleCertificate domain);

    List<VehicleCertificatePo> fromDomainList(List<VehicleCertificate> domainList);

    @Named("stringToCertificateStatus")
    default net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.CertificateStatus stringToCertificateStatus(String status) {
        if (status == null) {
            return null;
        }
        return net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.CertificateStatus.valOf(status);
    }

    @Named("certificateStatusToString")
    default String certificateStatusToString(net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.CertificateStatus status) {
        if (status == null) {
            return null;
        }
        return status.name();
    }
}
