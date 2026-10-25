package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.KeyProvisioningLease;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.KeyProvisioningLeasePo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * KL1 交付租约领域对象转换器（VMD-DSN-CR-069）
 *
 * @author hwyz_leo
 * @since 2026-10-25
 */
@Mapper
public interface KeyProvisioningLeaseConverter {

    KeyProvisioningLeaseConverter INSTANCE = Mappers.getMapper(KeyProvisioningLeaseConverter.class);

    KeyProvisioningLease toDomain(KeyProvisioningLeasePo po);

    List<KeyProvisioningLease> toDomainList(List<KeyProvisioningLeasePo> poList);

    KeyProvisioningLeasePo fromDomain(KeyProvisioningLease domain);
}
