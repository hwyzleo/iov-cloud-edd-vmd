package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePartHistory;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehiclePartHistoryPo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehiclePartPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 车辆零件相关领域对象转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehiclePartConverter {

    VehiclePartConverter INSTANCE = Mappers.getMapper(VehiclePartConverter.class);

    // ==================== 车辆零件 ====================

    VehiclePart toDomain(VehiclePartPo po);

    List<VehiclePart> toDomainList(List<VehiclePartPo> poList);

    VehiclePartPo fromDomain(VehiclePart domain);

    List<VehiclePartPo> fromDomainList(List<VehiclePart> domainList);

    // ==================== 车辆零件变更历史 ====================

    VehiclePartHistory toHistoryDomain(VehiclePartHistoryPo po);

    List<VehiclePartHistory> toHistoryDomainList(List<VehiclePartHistoryPo> poList);

    VehiclePartHistoryPo fromHistoryDomain(VehiclePartHistory domain);

    List<VehiclePartHistoryPo> fromHistoryDomainList(List<VehiclePartHistory> domainList);
}
