package net.hwyz.iov.cloud.edd.vmd.service.application.mapper;

import net.hwyz.iov.cloud.edd.vmd.api.vo.BaseModelFeatureCodeVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdVehBaseModelFeatureCodeDo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台基础车型特征值转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface BaseModelFeatureCodeMapper {

    BaseModelFeatureCodeMapper INSTANCE = Mappers.getMapper(BaseModelFeatureCodeMapper.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param vehBaseModelFeatureCodeDo 数据对象
     * @return 数据传输对象
     */
    @Mappings({
            @Mapping(target = "featureCode", expression = "java(net.hwyz.iov.cloud.framework.common.util.StrUtil.isBlank(vehBaseModelFeatureCodeDo.getFeatureCode()) ? null : vehBaseModelFeatureCodeDo.getFeatureCode().split(\",\"))")
    })
    BaseModelFeatureCodeVo fromDo(VmdVehBaseModelFeatureCodeDo vehBaseModelFeatureCodeDo);

    /**
     * 数据传输对象转数据对象
     *
     * @param baseModelFeatureCodeVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({
            @Mapping(target = "featureCode", expression = "java(baseModelFeatureCodeVo.getFeatureCode() == null ? null : java.lang.String.join(\",\", baseModelFeatureCodeVo.getFeatureCode()))")
    })
    VmdVehBaseModelFeatureCodeDo toDo(BaseModelFeatureCodeVo baseModelFeatureCodeVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param vehBaseModelFeatureCodeDoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<BaseModelFeatureCodeVo> fromDoList(List<VmdVehBaseModelFeatureCodeDo> vehBaseModelFeatureCodeDoList);

}
