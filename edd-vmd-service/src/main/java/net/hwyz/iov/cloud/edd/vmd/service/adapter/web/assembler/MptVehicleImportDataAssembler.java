package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.VehicleImportDataRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.VehicleImportDataResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleImportDataDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VehicleImportDataCmd;

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
    VehicleImportDataResponse fromDto(VehicleImportDataDto vehicleImportDataDto);

    /**
     * 导入数据 VO 转 DTO
     *
     * @param vehicleImportDataVo 导入数据 VO
     * @return 导入数据 DTO
     */
    VehicleImportDataDto toDto(VehicleImportDataRequest vehicleImportDataVo);
    /**
     * VO 转命令
     *
     * @param vo VO
     * @return 命令
     */
    VehicleImportDataCmd toCmd(VehicleImportDataRequest vo);


    /**
     * 导入数据 DTO 列表转 VO 列表
     *
     * @param vehicleImportDataDtoList 导入数据 DTO 列表
     * @return 导入数据 VO 列表
     */
    List<VehicleImportDataResponse> fromDtoList(List<VehicleImportDataDto> vehicleImportDataDtoList);

}
