package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.SeriesVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehSeriesPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
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
     * 数据对象转数据传输对象
     *
     * @param vehSeriesPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({
            @Mapping(source = "description", target = "description")
    })
    SeriesVo fromPo(VehSeriesPo vehSeriesPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param seriesVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({
            @Mapping(source = "description", target = "description")
    })
    VehSeriesPo toPo(SeriesVo seriesVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param vehSeriesPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<SeriesVo> fromPoList(List<VehSeriesPo> vehSeriesPoList);

}
