package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Variant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VariantHierarchy;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmVariantHierarchyPo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmVariantPo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 版本领域对象转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface VariantConverter {

    VariantConverter INSTANCE = Mappers.getMapper(VariantConverter.class);

    /**
     * PO 转领域对象
     *
     * @param mdmVariantPo PO
     * @return 领域对象
     */
    Variant toDomain(MdmVariantPo mdmVariantPo);

    /**
     * PO 列表转领域对象列表
     *
     * @param mdmVariantPoList PO 列表
     * @return 领域对象列表
     */
    List<Variant> toDomainList(List<MdmVariantPo> mdmVariantPoList);

    /**
     * 领域对象转 PO
     *
     * @param variant 领域对象
     * @return PO
     */
    MdmVariantPo fromDomain(Variant variant);

    /**
     * 产品树补全视图 PO 转领域值对象（CR-048 / RD-048-4）
     *
     * @param mdmVariantHierarchyPo 产品树补全视图 PO
     * @return 版本产品树补全值对象
     */
    VariantHierarchy toHierarchy(MdmVariantHierarchyPo mdmVariantHierarchyPo);

    /**
     * 产品树补全视图 PO 列表转领域值对象列表
     *
     * @param mdmVariantHierarchyPoList 产品树补全视图 PO 列表
     * @return 版本产品树补全值对象列表
     */
    List<VariantHierarchy> toHierarchyList(List<MdmVariantHierarchyPo> mdmVariantHierarchyPoList);
}
