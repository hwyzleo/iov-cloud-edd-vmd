package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.SupplierVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.SupplierDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台供应商 VO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface MptSupplierAssembler {

    MptSupplierAssembler INSTANCE = Mappers.getMapper(MptSupplierAssembler.class);

    /**
     * DTO 转 VO
     *
     * @param supplierDto DTO
     * @return VO
     */
    SupplierVo fromDto(SupplierDto supplierDto);

    /**
     * VO 转 DTO
     *
     * @param supplierVo VO
     * @return DTO
     */
    SupplierDto toDto(SupplierVo supplierVo);

    /**
     * DTO 列表转 VO 列表
     *
     * @param supplierDtoList DTO 列表
     * @return VO 列表
     */
    List<SupplierVo> fromDtoList(List<SupplierDto> supplierDtoList);

}
