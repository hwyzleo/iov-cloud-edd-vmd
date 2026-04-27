package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.SeriesVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.SeriesDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台车系 VO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface MptSeriesAssembler {

    MptSeriesAssembler INSTANCE = Mappers.getMapper(MptSeriesAssembler.class);

    /**
     * DTO 转 VO
     *
     * @param seriesDto DTO
     * @return VO
     */
    SeriesVo fromDto(SeriesDto seriesDto);

    /**
     * VO 转 DTO
     *
     * @param seriesVo VO
     * @return DTO
     */
    SeriesDto toDto(SeriesVo seriesVo);

    /**
     * DTO 列表转 VO 列表
     *
     * @param seriesDtoList DTO 列表
     * @return VO 列表
     */
    List<SeriesVo> fromDtoList(List<SeriesDto> seriesDtoList);

}
