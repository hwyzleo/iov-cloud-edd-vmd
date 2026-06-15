package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartImportData;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.PartImportDataPo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 零件导入数据转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface PartImportDataConverter {

    PartImportDataConverter INSTANCE = Mappers.getMapper(PartImportDataConverter.class);

    PartImportData toEntity(PartImportDataPo po);

    PartImportDataPo toPo(PartImportData entity);
}
