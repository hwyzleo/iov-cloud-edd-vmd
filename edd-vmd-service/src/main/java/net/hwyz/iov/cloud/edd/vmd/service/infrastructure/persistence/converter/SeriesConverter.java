package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Series;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehSeriesPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 车系领域对象转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface SeriesConverter {

    SeriesConverter INSTANCE = Mappers.getMapper(SeriesConverter.class);

    /**
     * PO 转领域对象
     *
     * @param vehSeriesPo PO
     * @return 领域对象
     */
    @Mapping(target = "state", ignore = true)
    Series toDomain(VehSeriesPo vehSeriesPo);

    /**
     * PO 列表转领域对象列表
     *
     * @param vehSeriesPoList PO 列表
     * @return 领域对象列表
     */
    List<Series> toDomainList(List<VehSeriesPo> vehSeriesPoList);

    /**
     * 领域对象转 PO
     *
     * @param series 领域对象
     * @return PO
     */
    @Mapping(target = "rowVersion", ignore = true)
    @Mapping(target = "rowValid", ignore = true)
    VehSeriesPo fromDomain(Series series);
}
