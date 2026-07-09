package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ProvFacilityDevice;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SecurityConstantState;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.ProvFacilityDevicePo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 安全灌注机注册转换器
 *
 * @author hwyz_leo
 * @since 2026-07-08
 */
@Mapper
public interface ProvFacilityDeviceConverter {

    ProvFacilityDeviceConverter INSTANCE = Mappers.getMapper(ProvFacilityDeviceConverter.class);

    /**
     * PO 转领域对象
     */
    ProvFacilityDevice toDomain(ProvFacilityDevicePo po);

    /**
     * PO 列表转领域对象列表
     */
    List<ProvFacilityDevice> toDomainList(List<ProvFacilityDevicePo> poList);

    /**
     * 领域对象转 PO
     */
    ProvFacilityDevicePo fromDomain(ProvFacilityDevice entity);

    /**
     * 领域对象列表转 PO 列表
     */
    List<ProvFacilityDevicePo> fromDomainList(List<ProvFacilityDevice> entityList);

    default SecurityConstantState mapStringToSecurityConstantState(String state) {
        return state != null ? SecurityConstantState.valOf(state) : null;
    }

    default String mapSecurityConstantStateToString(SecurityConstantState state) {
        return state != null ? state.getValue() : null;
    }
}
