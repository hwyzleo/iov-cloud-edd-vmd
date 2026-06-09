package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VariantFeatureCode;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehVariantFeatureCodePo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 版本特征关系领域对象转换器（原BaseModelFeatureCodeConverter，CR-016重命名）
 *
 * @author hwyz_leo
 */
@Mapper
public interface VariantFeatureCodeConverter {

    VariantFeatureCodeConverter INSTANCE = Mappers.getMapper(VariantFeatureCodeConverter.class);

    /**
     * PO 转领域对象
     *
     * @param po PO
     * @return 领域对象
     */
    @Mapping(target = "familyName", ignore = true)
    @Mapping(target = "featureName", ignore = true)
    VariantFeatureCode toDomain(VehVariantFeatureCodePo po);

    /**
     * PO 列表转领域对象列表
     *
     * @param poList PO 列表
     * @return 领域对象列表
     */
    List<VariantFeatureCode> toDomainList(List<VehVariantFeatureCodePo> poList);

    /**
     * 领域对象转 PO
     *
     * @param domain 领域对象
     * @return PO
     */
    VehVariantFeatureCodePo fromDomain(VariantFeatureCode domain);
}