package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehSecurityConstant;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehSecurityConstantPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 车辆安全常量转换器
 * 
 * @author hwyz_leo
 * @since 2026-06-17
 */
@Mapper
public interface VehSecurityConstantConverter {

    VehSecurityConstantConverter INSTANCE = Mappers.getMapper(VehSecurityConstantConverter.class);

    @Mapping(source = "presetState", target = "presetState")
    VehSecurityConstant toEntity(VehSecurityConstantPo po);

    @Mapping(source = "presetState", target = "presetState")
    VehSecurityConstantPo toPo(VehSecurityConstant entity);
}
