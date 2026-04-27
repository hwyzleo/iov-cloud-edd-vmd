package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.SeriesRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.SeriesResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.SeriesDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.SeriesCmd;

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
    SeriesResponse fromDto(SeriesDto seriesDto);

    /**
     * VO 转 DTO
     *
     * @param seriesVo VO
     * @return DTO
     */
    SeriesDto toDto(SeriesRequest seriesVo);
    /**
     * VO 转命令
     *
     * @param vo VO
     * @return 命令
     */
    SeriesCmd toCmd(SeriesRequest vo);


    /**
     * DTO 列表转 VO 列表
     *
     * @param seriesDtoList DTO 列表
     * @return VO 列表
     */
    List<SeriesResponse> fromDtoList(List<SeriesDto> seriesDtoList);

}
