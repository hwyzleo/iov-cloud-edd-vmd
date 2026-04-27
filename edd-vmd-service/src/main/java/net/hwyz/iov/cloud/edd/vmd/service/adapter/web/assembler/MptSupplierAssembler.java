package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.SupplierRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.SupplierResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.SupplierDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.SupplierCmd;

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
    SupplierResponse fromDto(SupplierDto supplierDto);

    /**
     * VO 转 DTO
     *
     * @param supplierVo VO
     * @return DTO
     */
    SupplierDto toDto(SupplierRequest supplierVo);
    /**
     * VO 转命令
     *
     * @param vo VO
     * @return 命令
     */
    SupplierCmd toCmd(SupplierRequest vo);


    /**
     * DTO 列表转 VO 列表
     *
     * @param supplierDtoList DTO 列表
     * @return VO 列表
     */
    List<SupplierResponse> fromDtoList(List<SupplierDto> supplierDtoList);

}
