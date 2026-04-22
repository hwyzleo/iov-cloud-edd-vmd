package net.hwyz.iov.cloud.edd.vmd.service.application.mapper;

import net.hwyz.iov.cloud.edd.vmd.api.vo.ModelVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehModelDo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台车型转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface ModelMapper {

    ModelMapper INSTANCE = Mappers.getMapper(ModelMapper.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param vehModelDo 数据对象
     * @return 数据传输对象
     */
    @Mappings({
            @Mapping(source = "description", target = "description")
    })
    ModelVo fromDo(VmdVehModelDo vehModelDo);

    /**
     * 数据传输对象转数据对象
     *
     * @param modelVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({
            @Mapping(source = "description", target = "description")
    })
    VmdVehModelDo toDo(ModelVo modelVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param vehModelDoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<ModelVo> fromDoList(List<VmdVehModelDo> vehModelDoList);

}
