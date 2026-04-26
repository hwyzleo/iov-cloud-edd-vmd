package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Supplier;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.SupplierPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 供应商领域对象转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface SupplierConverter {

    SupplierConverter INSTANCE = Mappers.getMapper(SupplierConverter.class);

    /**
     * PO 转领域对象
     *
     * @param supplierPo PO
     * @return 领域对象
     */
    Supplier toDomain(SupplierPo supplierPo);

    /**
     * PO 列表转领域对象列表
     *
     * @param supplierPoList PO 列表
     * @return 领域对象列表
     */
    List<Supplier> toDomainList(List<SupplierPo> supplierPoList);

    /**
     * 领域对象转 PO
     *
     * @param supplier 领域对象
     * @return PO
     */
    SupplierPo fromDomain(Supplier supplier);
}
