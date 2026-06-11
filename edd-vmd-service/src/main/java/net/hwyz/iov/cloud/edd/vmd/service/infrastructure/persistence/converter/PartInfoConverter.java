package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.InboundSourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.PartType;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.PartInfoPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 物理零件实例相关领域对象转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface PartInfoConverter {

    PartInfoConverter INSTANCE = Mappers.getMapper(PartInfoConverter.class);

    PartInfo toDomain(PartInfoPo po);

    List<PartInfo> toDomainList(List<PartInfoPo> poList);

    PartInfoPo fromDomain(PartInfo domain);

    List<PartInfoPo> fromDomainList(List<PartInfo> domainList);

    default InboundSourceType mapStringToInboundSourceType(String source) {
        return source != null ? InboundSourceType.valOf(source) : null;
    }

    default String mapInboundSourceTypeToString(InboundSourceType source) {
        return source != null ? source.getValue() : null;
    }

    default PartType mapStringToPartType(String partType) {
        return partType != null ? PartType.valOf(partType) : null;
    }

    default String mapPartTypeToString(PartType partType) {
        return partType != null ? partType.getValue() : null;
    }
}
