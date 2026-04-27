package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.ManufacturerRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.ManufacturerResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ManufacturerCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ManufacturerDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台生产厂商 VO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface MptManufacturerAssembler {

    MptManufacturerAssembler INSTANCE = Mappers.getMapper(MptManufacturerAssembler.class);

    /**
     * DTO 转 VO
     *
     * @param manufacturerDto DTO
     * @return VO
     */
    ManufacturerResponse fromDto(ManufacturerDto manufacturerDto);

    /**
     * VO 转 DTO
     *
     * @param manufacturerVo VO
     * @return DTO
     */
    ManufacturerDto toDto(ManufacturerRequest manufacturerVo);

    /**
     * VO 转 CMD
     *
     * @param manufacturerVo VO
     * @return CMD
     */
    ManufacturerCmd toCmd(ManufacturerRequest manufacturerVo);

    /**
     * DTO 列表转 VO 列表
     *
     * @param manufacturerDtoList DTO 列表
     * @return VO 列表
     */
    List<ManufacturerResponse> fromDtoList(List<ManufacturerDto> manufacturerDtoList);

}
