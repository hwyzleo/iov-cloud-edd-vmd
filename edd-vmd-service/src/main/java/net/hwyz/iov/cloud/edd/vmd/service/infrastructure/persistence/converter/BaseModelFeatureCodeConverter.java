package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BaseModelFeatureCode;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehBaseModelFeatureCodePo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 基础车型特征关系领域对象转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface BaseModelFeatureCodeConverter {

    BaseModelFeatureCodeConverter INSTANCE = Mappers.getMapper(BaseModelFeatureCodeConverter.class);

    /**
     * PO 转领域对象
     *
     * @param po PO
     * @return 领域对象
     */
    @Mapping(target = "familyName", ignore = true)
    @Mapping(target = "featureName", ignore = true)
    BaseModelFeatureCode toDomain(VehBaseModelFeatureCodePo po);

    /**
     * PO 列表转领域对象列表
     *
     * @param poList PO 列表
     * @return 领域对象列表
     */
    List<BaseModelFeatureCode> toDomainList(List<VehBaseModelFeatureCodePo> poList);

    /**
     * 领域对象转 PO
     *
     * @param domain 领域对象
     * @return PO
     */
    VehBaseModelFeatureCodePo fromDomain(BaseModelFeatureCode domain);
}
