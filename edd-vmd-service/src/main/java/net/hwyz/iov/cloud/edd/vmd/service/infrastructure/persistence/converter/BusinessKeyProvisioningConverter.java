package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BusinessKeyProvisioning;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyProvisioningState;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.BusinessKeyProvisioningPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 业务密钥多用途开通批次领域对象转换器（CR-058）
 *
 * @author hwyz_leo
 * @since 2026-10-12
 */
@Mapper
public interface BusinessKeyProvisioningConverter {

    BusinessKeyProvisioningConverter INSTANCE = Mappers.getMapper(BusinessKeyProvisioningConverter.class);

    @Mapping(source = "status", target = "status", qualifiedByName = "stringToProvisioningState")
    BusinessKeyProvisioning toDomain(BusinessKeyProvisioningPo po);

    List<BusinessKeyProvisioning> toDomainList(List<BusinessKeyProvisioningPo> poList);

    @Mapping(source = "status", target = "status", qualifiedByName = "provisioningStateToString")
    BusinessKeyProvisioningPo fromDomain(BusinessKeyProvisioning domain);

    List<BusinessKeyProvisioningPo> fromDomainList(List<BusinessKeyProvisioning> domainList);

    @Named("stringToProvisioningState")
    default BusinessKeyProvisioningState stringToProvisioningState(String state) {
        return state != null ? BusinessKeyProvisioningState.valOf(state) : null;
    }

    @Named("provisioningStateToString")
    default String provisioningStateToString(BusinessKeyProvisioningState state) {
        return state != null ? state.getValue() : null;
    }
}
