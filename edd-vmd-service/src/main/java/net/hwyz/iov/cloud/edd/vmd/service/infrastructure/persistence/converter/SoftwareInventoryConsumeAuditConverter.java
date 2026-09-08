package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.SoftwareInventoryConsumeAudit;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.SoftwareInventoryConsumeAuditPo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * OTA 车辆软件观测消费审计 领域对象转换器
 *
 * @author hwyz_leo
 * @since 2026-09-08
 */
@Mapper
public interface SoftwareInventoryConsumeAuditConverter {

    SoftwareInventoryConsumeAuditConverter INSTANCE = Mappers.getMapper(SoftwareInventoryConsumeAuditConverter.class);

    SoftwareInventoryConsumeAudit toDomain(SoftwareInventoryConsumeAuditPo po);

    List<SoftwareInventoryConsumeAudit> toDomainList(List<SoftwareInventoryConsumeAuditPo> poList);

    SoftwareInventoryConsumeAuditPo fromDomain(SoftwareInventoryConsumeAudit domain);

    List<SoftwareInventoryConsumeAuditPo> fromDomainList(List<SoftwareInventoryConsumeAudit> domainList);
}
