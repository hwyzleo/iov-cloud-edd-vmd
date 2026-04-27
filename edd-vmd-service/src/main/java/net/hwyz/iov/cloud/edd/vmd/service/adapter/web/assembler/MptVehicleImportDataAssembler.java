package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.VehicleImportDataVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.VehicleImportDataDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台车辆导入数据 VO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface MptVehicleImportDataAssembler {

    MptVehicleImportDataAssembler INSTANCE = Mappers.getMapper(MptVehicleImportDataAssembler.class);

    /**
     * 导入数据 DTO 转 VO
     *
     * @param vehicleImportDataDto 导入数据 DTO
     * @return 导入数据 VO
     */
    VehicleImportDataVo fromDto(VehicleImportDataDto vehicleImportDataDto);

    /**
     * 导入数据 VO 转 DTO
     *
     * @param vehicleImportDataVo 导入数据 VO
     * @return 导入数据 DTO
     */
    VehicleImportDataDto toDto(VehicleImportDataVo vehicleImportDataVo);

    /**
     * 导入数据 DTO 列表转 VO 列表
     *
     * @param vehicleImportDataDtoList 导入数据 DTO 列表
     * @return 导入数据 VO 列表
     */
    List<VehicleImportDataVo> fromDtoList(List<VehicleImportDataDto> vehicleImportDataDtoList);

}
