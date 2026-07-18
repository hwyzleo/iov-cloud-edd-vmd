package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartSoftwareInstallation;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.PartSoftwareInstallationPo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 软件实装时态记录领域对象转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface PartSoftwareInstallationConverter {

    PartSoftwareInstallationConverter INSTANCE = Mappers.getMapper(PartSoftwareInstallationConverter.class);

    PartSoftwareInstallation toDomain(PartSoftwareInstallationPo po);

    List<PartSoftwareInstallation> toDomainList(List<PartSoftwareInstallationPo> poList);

    PartSoftwareInstallationPo fromDomain(PartSoftwareInstallation domain);

    List<PartSoftwareInstallationPo> fromDomainList(List<PartSoftwareInstallation> domainList);
}
