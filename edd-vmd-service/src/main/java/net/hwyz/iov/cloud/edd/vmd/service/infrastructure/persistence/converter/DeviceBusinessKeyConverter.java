package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.DeviceBusinessKey;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BusinessKeyState;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.DeviceBusinessKeyPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 设备业务密钥目录领域对象转换器
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@Mapper
public interface DeviceBusinessKeyConverter {

    DeviceBusinessKeyConverter INSTANCE = Mappers.getMapper(DeviceBusinessKeyConverter.class);

    @Mapping(source = "keyState", target = "keyState", qualifiedByName = "stringToBusinessKeyState")
    DeviceBusinessKey toDomain(DeviceBusinessKeyPo po);

    List<DeviceBusinessKey> toDomainList(List<DeviceBusinessKeyPo> poList);

    @Mapping(source = "keyState", target = "keyState", qualifiedByName = "businessKeyStateToString")
    DeviceBusinessKeyPo fromDomain(DeviceBusinessKey domain);

    List<DeviceBusinessKeyPo> fromDomainList(List<DeviceBusinessKey> domainList);

    @Named("stringToBusinessKeyState")
    default BusinessKeyState stringToBusinessKeyState(String state) {
        return state != null ? BusinessKeyState.valOf(state) : null;
    }

    @Named("businessKeyStateToString")
    default String businessKeyStateToString(BusinessKeyState state) {
        return state != null ? state.getValue() : null;
    }
}
