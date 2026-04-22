package net.hwyz.iov.cloud.edd.vmd.service.application.mapper;

import net.hwyz.iov.cloud.edd.vmd.api.vo.BaseModelVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehBaseModelDo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台基础车型转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface BaseModelMapper {

    BaseModelMapper INSTANCE = Mappers.getMapper(BaseModelMapper.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param vehBaseModelDo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    BaseModelVo fromDo(VmdVehBaseModelDo vehBaseModelDo);

    /**
     * 数据传输对象转数据对象
     *
     * @param baseModelVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    VmdVehBaseModelDo toDo(BaseModelVo baseModelVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param vehBaseModelDoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<BaseModelVo> fromDoList(List<VmdVehBaseModelDo> vehBaseModelDoList);

}
