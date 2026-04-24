package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.SupplierVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.SupplierPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
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
     * 数据对象转数据传输对象
     *
     * @param supplierPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    SupplierVo fromPo(SupplierPo supplierPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param supplierVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    SupplierPo toPo(SupplierVo supplierVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param supplierPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<SupplierVo> fromPoList(List<SupplierPo> supplierPoList);

}
