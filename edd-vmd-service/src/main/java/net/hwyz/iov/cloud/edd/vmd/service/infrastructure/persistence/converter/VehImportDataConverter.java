package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportData;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehImportDataPo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 车辆导入数据转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehImportDataConverter {

    VehImportDataConverter INSTANCE = Mappers.getMapper(VehImportDataConverter.class);

    VehImportData toEntity(VehImportDataPo po);

    VehImportDataPo toPo(VehImportData entity);
}
