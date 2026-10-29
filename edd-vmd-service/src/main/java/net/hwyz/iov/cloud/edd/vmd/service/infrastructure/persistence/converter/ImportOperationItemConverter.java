package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ImportOperationItem;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.ImportOperationItemPo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 统一导入操作明细实体转换器（CR-071）
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
@Mapper
public interface ImportOperationItemConverter {

    ImportOperationItemConverter INSTANCE = Mappers.getMapper(ImportOperationItemConverter.class);

    ImportOperationItem toDomain(ImportOperationItemPo po);

    List<ImportOperationItem> toDomainList(List<ImportOperationItemPo> poList);

    ImportOperationItemPo fromDomain(ImportOperationItem domain);

    List<ImportOperationItemPo> fromDomainList(List<ImportOperationItem> domainList);

}
