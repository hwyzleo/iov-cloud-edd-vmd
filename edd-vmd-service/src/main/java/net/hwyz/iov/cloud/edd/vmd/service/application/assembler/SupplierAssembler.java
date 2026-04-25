package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.SupplierVo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Supplier;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台供应商转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface SupplierAssembler {

    SupplierAssembler INSTANCE = Mappers.getMapper(SupplierAssembler.class);

    /**
     * 领域对象转数据传输对象
     *
     * @param supplier 领域对象
     * @return 数据传输对象
     */
    SupplierVo fromDomain(Supplier supplier);

    /**
     * 数据传输对象转领域对象
     *
     * @param supplierVo 数据传输对象
     * @return 领域对象
     */
    Supplier toDomain(SupplierVo supplierVo);

    /**
     * 领域对象列表转数据传输对象列表
     *
     * @param supplierList 领域对象列表
     * @return 数据传输对象列表
     */
    List<SupplierVo> fromDomainList(List<Supplier> supplierList);

}
