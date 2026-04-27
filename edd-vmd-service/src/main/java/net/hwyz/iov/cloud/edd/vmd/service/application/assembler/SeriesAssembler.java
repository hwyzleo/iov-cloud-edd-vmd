package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.SeriesDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Series;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 车系 DTO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface SeriesAssembler {

    SeriesAssembler INSTANCE = Mappers.getMapper(SeriesAssembler.class);

    /**
     * 领域对象转 DTO
     *
     * @param series 领域对象
     * @return DTO
     */
    SeriesDto fromDomain(Series series);

    /**
     * DTO 转领域对象
     *
     * @param seriesDto DTO
     * @return 领域对象
     */
    Series toDomain(SeriesDto seriesDto);

    /**
     * 领域对象列表转 DTO 列表
     *
     * @param seriesList 领域对象列表
     * @return DTO 列表
     */
    List<SeriesDto> fromDomainList(List<Series> seriesList);

}
