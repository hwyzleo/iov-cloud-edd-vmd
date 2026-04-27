package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleImportDataDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleImportData;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VehicleImportDataCmd;

import java.util.List;

/**
 * 车辆导入数据 DTO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface VehicleImportDataAssembler {

    VehicleImportDataAssembler INSTANCE = Mappers.getMapper(VehicleImportDataAssembler.class);

    /**
     * 领域对象转 DTO
     *
     * @param vehicleImportData 领域对象
     * @return DTO
     */
    VehicleImportDataDto fromDomain(VehicleImportData vehicleImportData);

    /**
     * DTO 转领域对象
     *
     * @param vehicleImportDataDto DTO
     * @return 领域对象
     */
    VehicleImportData toDomain(VehicleImportDataDto vehicleImportDataDto);
    /**
     * 命令转领域对象
     *
     * @param cmd 命令
     * @return 领域对象
     */
    VehicleImportData toDomain(VehicleImportDataCmd cmd);


    /**
     * 领域对象列表转 DTO 列表
     *
     * @param vehicleImportDataList 领域对象列表
     * @return DTO 列表
     */
    List<VehicleImportDataDto> fromDomainList(List<VehicleImportData> vehicleImportDataList);

}
