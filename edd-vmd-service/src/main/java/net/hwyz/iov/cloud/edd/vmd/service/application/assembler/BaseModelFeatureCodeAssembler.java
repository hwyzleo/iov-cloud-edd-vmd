package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.BaseModelFeatureCodeVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehBaseModelFeatureCodePo;
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
public interface BaseModelFeatureCodeAssembler {

    BaseModelFeatureCodeAssembler INSTANCE = Mappers.getMapper(BaseModelFeatureCodeAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param vehBaseModelFeatureCodePo 数据对象
     * @return 数据传输对象
     */
    @Mappings({
            @Mapping(target = "featureCode", expression = "java(net.hwyz.iov.cloud.framework.common.util.StrUtil.isBlank(vehBaseModelFeatureCodePo.getFeatureCode()) ? null : vehBaseModelFeatureCodePo.getFeatureCode().split(\",\"))")
    })
    BaseModelFeatureCodeVo fromPo(VehBaseModelFeatureCodePo vehBaseModelFeatureCodePo);

    /**
     * 数据传输对象转数据对象
     *
     * @param baseModelFeatureCodeVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({
            @Mapping(target = "featureCode", expression = "java(baseModelFeatureCodeVo.getFeatureCode() == null ? null : java.lang.String.join(\",\", baseModelFeatureCodeVo.getFeatureCode()))")
    })
    VehBaseModelFeatureCodePo toPo(BaseModelFeatureCodeVo baseModelFeatureCodeVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param vehBaseModelFeatureCodePoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<BaseModelFeatureCodeVo> fromPoList(List<VehBaseModelFeatureCodePo> vehBaseModelFeatureCodePoList);

}
