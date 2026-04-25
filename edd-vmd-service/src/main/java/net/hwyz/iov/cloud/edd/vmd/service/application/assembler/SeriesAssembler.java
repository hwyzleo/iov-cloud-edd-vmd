package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.SeriesVo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Series;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台车系转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface SeriesAssembler {

    SeriesAssembler INSTANCE = Mappers.getMapper(SeriesAssembler.class);

    /**
     * 领域对象转数据传输对象
     *
     * @param series 领域对象
     * @return 数据传输对象
     */
    SeriesVo fromDomain(Series series);

    /**
     * 数据传输对象转领域对象
     *
     * @param seriesVo 数据传输对象
     * @return 领域对象
     */
    Series toDomain(SeriesVo seriesVo);

    /**
     * 领域对象列表转数据传输对象列表
     *
     * @param seriesList 领域对象列表
     * @return 数据传输对象列表
     */
    List<SeriesVo> fromDomainList(List<Series> seriesList);

}
