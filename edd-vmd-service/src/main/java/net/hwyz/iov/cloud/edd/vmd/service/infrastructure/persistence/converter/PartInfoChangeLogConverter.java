package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfoChangeLog;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.PartInfoChangeLogPo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 零件实例属性变更历史领域对象转换器（CR-071）
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
@Mapper
public interface PartInfoChangeLogConverter {

    PartInfoChangeLogConverter INSTANCE = Mappers.getMapper(PartInfoChangeLogConverter.class);

    PartInfoChangeLog toDomain(PartInfoChangeLogPo po);

    List<PartInfoChangeLog> toDomainList(List<PartInfoChangeLogPo> poList);

    PartInfoChangeLogPo fromDomain(PartInfoChangeLog domain);

}
