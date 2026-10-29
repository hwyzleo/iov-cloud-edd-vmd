package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleInfoChangeLog;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehicleInfoChangeLogPo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 车辆基础信息属性变更历史领域对象转换器（CR-071）
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
@Mapper
public interface VehicleInfoChangeLogConverter {

    VehicleInfoChangeLogConverter INSTANCE = Mappers.getMapper(VehicleInfoChangeLogConverter.class);

    VehicleInfoChangeLog toDomain(VehicleInfoChangeLogPo po);

    List<VehicleInfoChangeLog> toDomainList(List<VehicleInfoChangeLogPo> poList);

    VehicleInfoChangeLogPo fromDomain(VehicleInfoChangeLog domain);

}
