package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.VehImportDataRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.ReplayActionPreviewResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.VehImportDataResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.VehicleImportReplayPreviewResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VehImportDataCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ReplayActionPreview;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehImportDataDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleImportReplayPreview;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 车辆导入数据装配器
 *
 * @author hwyz_leo
 * @since 2026-06-16
 */
@Mapper
public interface MptVehImportDataAssembler {

    MptVehImportDataAssembler INSTANCE = Mappers.getMapper(MptVehImportDataAssembler.class);

    VehImportDataResponse fromDto(VehImportDataDto dto);

    VehImportDataCmd toCmd(VehImportDataRequest request);

    /**
     * 车辆导入补发预检 DTO → 响应
     * <p>
     * VMD-DSN-CR-057: 车辆导入补发扩展
     */
    VehicleImportReplayPreviewResponse fromPreview(VehicleImportReplayPreview preview);

    ReplayActionPreviewResponse fromPreviewAction(ReplayActionPreview preview);
}
