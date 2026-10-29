package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ImportOperation;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.ImportOperationPo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 统一导入操作主实体转换器（CR-071）
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
@Mapper
public interface ImportOperationConverter {

    ImportOperationConverter INSTANCE = Mappers.getMapper(ImportOperationConverter.class);

    ImportOperation toDomain(ImportOperationPo po);

    List<ImportOperation> toDomainList(List<ImportOperationPo> poList);

    ImportOperationPo fromDomain(ImportOperation domain);

}
