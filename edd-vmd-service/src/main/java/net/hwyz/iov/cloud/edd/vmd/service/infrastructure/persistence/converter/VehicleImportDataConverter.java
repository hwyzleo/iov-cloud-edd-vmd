package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleImportData;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehImportDataPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 车辆导入数据领域对象转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleImportDataConverter {

    VehicleImportDataConverter INSTANCE = Mappers.getMapper(VehicleImportDataConverter.class);

    /**
     * PO 转领域对象
     *
     * @param vehImportDataPo PO
     * @return 领域对象
     */
    VehicleImportData toDomain(VehImportDataPo vehImportDataPo);

    /**
     * PO 列表转领域对象列表
     *
     * @param vehImportDataPoList PO 列表
     * @return 领域对象列表
     */
    List<VehicleImportData> toDomainList(List<VehImportDataPo> vehImportDataPoList);

    /**
     * 领域对象转 PO
     *
     * @param vehicleImportData 领域对象
     * @return PO
     */
    VehImportDataPo fromDomain(VehicleImportData vehicleImportData);
}
