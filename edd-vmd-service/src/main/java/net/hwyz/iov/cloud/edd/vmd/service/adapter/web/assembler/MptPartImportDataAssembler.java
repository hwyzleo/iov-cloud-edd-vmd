package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.PartImportDataRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.PartImportDataResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.PartImportDataCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.PartImportDataDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 零件导入数据装配器
 *
 * @author hwyz_leo
 */
@Mapper
public interface MptPartImportDataAssembler {

    MptPartImportDataAssembler INSTANCE = Mappers.getMapper(MptPartImportDataAssembler.class);

    PartImportDataResponse fromDto(PartImportDataDto dto);

    PartImportDataCmd toCmd(PartImportDataRequest request);

    List<PartImportDataResponse> fromDtoList(List<PartImportDataDto> dtoList);
}
