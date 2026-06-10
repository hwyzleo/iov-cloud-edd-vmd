package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VariantOptionCode;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmVariantOptionCodePo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 版本选项值关系领域对象转换器（原BaseModelFeatureCodeConverter→VariantFeatureCodeConverter，CR-018重命名）
 *
 * @author hwyz_leo
 */
@Mapper
public interface VariantOptionCodeConverter {

    VariantOptionCodeConverter INSTANCE = Mappers.getMapper(VariantOptionCodeConverter.class);

    /**
     * PO 转领域对象
     *
     * @param po PO
     * @return 领域对象
     */
    @Mapping(target = "optionFamilyName", ignore = true)
    @Mapping(target = "optionName", ignore = true)
    VariantOptionCode toDomain(MdmVariantOptionCodePo po);

    /**
     * PO 列表转领域对象列表
     *
     * @param poList PO 列表
     * @return 领域对象列表
     */
    List<VariantOptionCode> toDomainList(List<MdmVariantOptionCodePo> poList);

    /**
     * 领域对象转 PO
     *
     * @param domain 领域对象
     * @return PO
     */
    MdmVariantOptionCodePo fromDomain(VariantOptionCode domain);
}
