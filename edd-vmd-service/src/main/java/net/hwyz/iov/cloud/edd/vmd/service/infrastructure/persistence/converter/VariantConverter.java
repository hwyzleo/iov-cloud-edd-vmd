package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Variant;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehVariantPo;
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
     * @param vehVariantPo PO
     * @return 领域对象
     */
    Variant toDomain(VehVariantPo vehVariantPo);

    /**
     * PO 列表转领域对象列表
     *
     * @param vehVariantPoList PO 列表
     * @return 领域对象列表
     */
    List<Variant> toDomainList(List<VehVariantPo> vehVariantPoList);

    /**
     * 领域对象转 PO
     *
     * @param variant 领域对象
     * @return PO
     */
    VehVariantPo fromDomain(Variant variant);
}
