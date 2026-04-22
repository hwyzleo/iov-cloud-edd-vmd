package net.hwyz.iov.cloud.edd.vmd.service.application.mapper;

import net.hwyz.iov.cloud.edd.vmd.api.vo.SeriesVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehSeriesDo;
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
public interface SeriesMapper {

    SeriesMapper INSTANCE = Mappers.getMapper(SeriesMapper.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param vehSeriesDo 数据对象
     * @return 数据传输对象
     */
    @Mappings({
            @Mapping(source = "description", target = "description")
    })
    SeriesVo fromDo(VmdVehSeriesDo vehSeriesDo);

    /**
     * 数据传输对象转数据对象
     *
     * @param seriesVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({
            @Mapping(source = "description", target = "description")
    })
    VmdVehSeriesDo toDo(SeriesVo seriesVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param vehSeriesDoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<SeriesVo> fromDoList(List<VmdVehSeriesDo> vehSeriesDoList);

}
