package net.hwyz.iov.cloud.edd.vmd.service.application.mapper;

import net.hwyz.iov.cloud.edd.vmd.api.vo.SupplierVo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.repository.dao.dataobject.VmdSupplierDo;
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
public interface SupplierMapper {

    SupplierMapper INSTANCE = Mappers.getMapper(SupplierMapper.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param supplierDo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    SupplierVo fromDo(VmdSupplierDo supplierDo);

    /**
     * 数据传输对象转数据对象
     *
     * @param supplierVo 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    VmdSupplierDo toDo(SupplierVo supplierVo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param supplierDoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<SupplierVo> fromDoList(List<VmdSupplierDo> supplierDoList);

}
