package net.hwyz.iov.cloud.edd.vmd.service.application.mapper;

import net.hwyz.iov.cloud.edd.vmd.api.vo.FeatureCodeVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehFeatureCodeDo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台车辆特征值转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface FeatureCodeMapper {

    FeatureCodeMapper INSTANCE = Mappers.getMapper(FeatureCodeMapper.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param vehFeatureCodeDo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    FeatureCodeVo fromDo(VmdVehFeatureCodeDo vehFeatureCodeDo);

    /**
     * 数据传输对象转数据对象
     *
     * @param featureCodeVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    VmdVehFeatureCodeDo toDo(FeatureCodeVo featureCodeVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param vehFeatureCodeDoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<FeatureCodeVo> fromDoList(List<VmdVehFeatureCodeDo> vehFeatureCodeDoList);

}
