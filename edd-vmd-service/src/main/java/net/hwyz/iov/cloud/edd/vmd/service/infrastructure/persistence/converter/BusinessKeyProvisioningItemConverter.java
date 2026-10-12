package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BusinessKeyProvisioningItem;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.ProvisioningDeviceResult;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.BusinessKeyProvisioningItemPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 业务密钥开通批次 ACK 明细领域对象转换器（CR-058）
 *
 * @author hwyz_leo
 * @since 2026-10-12
 */
@Mapper
public interface BusinessKeyProvisioningItemConverter {

    BusinessKeyProvisioningItemConverter INSTANCE = Mappers.getMapper(BusinessKeyProvisioningItemConverter.class);

    @Mapping(source = "deviceResult", target = "deviceResult", qualifiedByName = "stringToDeviceResult")
    BusinessKeyProvisioningItem toDomain(BusinessKeyProvisioningItemPo po);

    List<BusinessKeyProvisioningItem> toDomainList(List<BusinessKeyProvisioningItemPo> poList);

    @Mapping(source = "deviceResult", target = "deviceResult", qualifiedByName = "deviceResultToString")
    BusinessKeyProvisioningItemPo fromDomain(BusinessKeyProvisioningItem domain);

    List<BusinessKeyProvisioningItemPo> fromDomainList(List<BusinessKeyProvisioningItem> domainList);

    @Named("stringToDeviceResult")
    default ProvisioningDeviceResult stringToDeviceResult(String result) {
        return result != null ? ProvisioningDeviceResult.valOf(result) : null;
    }

    @Named("deviceResultToString")
    default String deviceResultToString(ProvisioningDeviceResult result) {
        return result != null ? result.getValue() : null;
    }
}
