package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.VehicleImportDataVo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleImportData;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 车辆导入数据转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleImportDataAssembler {

    VehicleImportDataAssembler INSTANCE = Mappers.getMapper(VehicleImportDataAssembler.class);

    /**
     * 领域对象转数据传输对象
     *
     * @param vehicleImportData 领域对象
     * @return 数据传输对象
     */
    VehicleImportDataVo fromDomain(VehicleImportData vehicleImportData);

    /**
     * 数据传输对象转领域对象
     *
     * @param vehicleImportDataVo 数据传输对象
     * @return 领域对象
     */
    VehicleImportData toDomain(VehicleImportDataVo vehicleImportDataVo);

    /**
     * 领域对象列表转数据传输对象列表
     *
     * @param vehicleImportDataList 领域对象列表
     * @return 数据传输对象列表
     */
    List<VehicleImportDataVo> fromDomainList(List<VehicleImportData> vehicleImportDataList);

}
