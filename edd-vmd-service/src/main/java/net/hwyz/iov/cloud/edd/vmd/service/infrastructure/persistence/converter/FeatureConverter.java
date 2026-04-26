package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.FeatureCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.FeatureFamily;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehFeatureCodePo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehFeatureFamilyPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 特征相关领域对象转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface FeatureConverter {

    FeatureConverter INSTANCE = Mappers.getMapper(FeatureConverter.class);

    // ==================== 特征族 ====================

    FeatureFamily toFamilyDomain(VehFeatureFamilyPo po);

    List<FeatureFamily> toFamilyDomainList(List<VehFeatureFamilyPo> poList);

    VehFeatureFamilyPo fromFamilyDomain(FeatureFamily domain);

    // ==================== 特征值 ====================

    FeatureCode toCodeDomain(VehFeatureCodePo po);

    List<FeatureCode> toCodeDomainList(List<VehFeatureCodePo> poList);

    VehFeatureCodePo fromCodeDomain(FeatureCode domain);
}
