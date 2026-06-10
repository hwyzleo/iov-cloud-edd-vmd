package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.SupplierDto;
// import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Supplier; // TODO: CR-019 - Supplier领域对象已删除
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.SupplierCmd;

import java.util.List;

/**
 * 供应商 DTO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface SupplierAssembler {

    SupplierAssembler INSTANCE = Mappers.getMapper(SupplierAssembler.class);

    // TODO: CR-019 - Supplier领域对象已删除，以下方法需要重新设计
    // /**
    //  * 领域对象转 DTO
    //  *
    //  * @param supplier 领域对象
    //  * @return DTO
    //  */
    // SupplierDto fromDomain(Supplier supplier);

    // /**
    //  * DTO 转领域对象
    //  *
    //  * @param supplierDto DTO
    //  * @return 领域对象
    //  */
    // Supplier toDomain(SupplierDto supplierDto);
    // /**
    //  * 命令转领域对象
    //  *
    //  * @param cmd 命令
    //  * @return 领域对象
    //  */
    // Supplier toDomain(SupplierCmd cmd);


    // /**
    //  * 领域对象列表转 DTO 列表
    //  *
    //  * @param supplierList 领域对象列表
    //  * @return DTO 列表
    //  */
    // List<SupplierDto> fromDomainList(List<Supplier> supplierList);

}
