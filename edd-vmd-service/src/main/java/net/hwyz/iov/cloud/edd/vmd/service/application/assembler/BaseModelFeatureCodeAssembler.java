package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.BaseModelFeatureCodeVo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BaseModelFeatureCode;
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
     * 领域对象转数据传输对象
     *
     * @param baseModelFeatureCode 领域对象
     * @return 数据传输对象
     */
    @Mappings({
            @Mapping(target = "featureCode", expression = "java(net.hwyz.iov.cloud.framework.common.util.StrUtil.isBlank(baseModelFeatureCode.getFeatureCode()) ? null : baseModelFeatureCode.getFeatureCode().split(\",\"))"),
            @Mapping(target = "featureName", expression = "java(net.hwyz.iov.cloud.framework.common.util.StrUtil.isBlank(baseModelFeatureCode.getFeatureName()) ? null : baseModelFeatureCode.getFeatureName().split(\",\"))")
    })
    BaseModelFeatureCodeVo fromDomain(BaseModelFeatureCode baseModelFeatureCode);

    /**
     * 数据传输对象转领域对象
     *
     * @param baseModelFeatureCodeVo 数据传输对象
     * @return 领域对象
     */
    @Mappings({
            @Mapping(target = "featureCode", expression = "java(baseModelFeatureCodeVo.getFeatureCode() == null ? null : String.join(\",\", baseModelFeatureCodeVo.getFeatureCode()))"),
            @Mapping(target = "featureName", expression = "java(baseModelFeatureCodeVo.getFeatureName() == null ? null : String.join(\",\", baseModelFeatureCodeVo.getFeatureName()))")
    })
    BaseModelFeatureCode toDomain(BaseModelFeatureCodeVo baseModelFeatureCodeVo);

    /**
     * 领域对象列表转数据传输对象列表
     *
     * @param baseModelFeatureCodeList 领域对象列表
     * @return 数据传输对象列表
     */
    List<BaseModelFeatureCodeVo> fromDomainList(List<BaseModelFeatureCode> baseModelFeatureCodeList);

}
