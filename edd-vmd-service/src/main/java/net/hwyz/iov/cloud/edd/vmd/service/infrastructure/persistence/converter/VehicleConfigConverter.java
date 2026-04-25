package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleConfig;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleConfigItem;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehicleConfigItemPo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehicleConfigPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 车辆配置相关领域对象转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleConfigConverter {

    VehicleConfigConverter INSTANCE = Mappers.getMapper(VehicleConfigConverter.class);

    // ==================== 车辆配置 ====================

    @Mapping(target = "state", ignore = true)
    @Mapping(target = "configState", source = "state")
    VehicleConfig toDomain(VehicleConfigPo po);

    List<VehicleConfig> toDomainList(List<VehicleConfigPo> poList);

    @Mapping(target = "state", source = "configState")
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "rowValid", ignore = true)
    VehicleConfigPo fromDomain(VehicleConfig domain);

    // ==================== 车辆配置项 ====================

    @Mapping(target = "state", ignore = true)
    VehicleConfigItem toItemDomain(VehicleConfigItemPo po);

    List<VehicleConfigItem> toItemDomainList(List<VehicleConfigItemPo> poList);

    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "rowValid", ignore = true)
    VehicleConfigItemPo fromItemDomain(VehicleConfigItem domain);
}
