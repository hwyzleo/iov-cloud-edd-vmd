package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.ManufacturerVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.ManufacturerDto;
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
    ManufacturerVo fromDto(ManufacturerDto manufacturerDto);

    /**
     * VO 转 DTO
     *
     * @param manufacturerVo VO
     * @return DTO
     */
    ManufacturerDto toDto(ManufacturerVo manufacturerVo);

    /**
     * DTO 列表转 VO 列表
     *
     * @param manufacturerDtoList DTO 列表
     * @return VO 列表
     */
    List<ManufacturerVo> fromDtoList(List<ManufacturerDto> manufacturerDtoList);

}
