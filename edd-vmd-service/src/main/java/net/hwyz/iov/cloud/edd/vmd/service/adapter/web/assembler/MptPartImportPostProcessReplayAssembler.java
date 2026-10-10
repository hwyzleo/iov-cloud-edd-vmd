package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.ReplayPartImportPostProcessRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.PartImportPostProcessReplayItemResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.PartImportPostProcessReplayResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ReplayPartImportPostProcessCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.PartImportPostProcessReplayDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.PartImportPostProcessReplayItemDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 零件导入后置处理重放装配器
 * <p>
 * VMD-DSN-CR-056: 零件导入后置处理人工重放（US-062）
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Mapper
public interface MptPartImportPostProcessReplayAssembler {

    MptPartImportPostProcessReplayAssembler INSTANCE = Mappers.getMapper(MptPartImportPostProcessReplayAssembler.class);

    ReplayPartImportPostProcessCmd toCmd(ReplayPartImportPostProcessRequest request);

    PartImportPostProcessReplayResponse fromDto(PartImportPostProcessReplayDto dto);

    PartImportPostProcessReplayItemResponse fromItemDto(PartImportPostProcessReplayItemDto dto);
}
