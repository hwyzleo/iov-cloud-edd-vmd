package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.BrandRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.BrandResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.BrandCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BrandDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台品牌 VO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface MptBrandAssembler {

    MptBrandAssembler INSTANCE = Mappers.getMapper(MptBrandAssembler.class);

    /**
     * DTO 转 Response
     *
     * @param brandDto DTO
     * @return Response
     */
    BrandResponse fromDto(BrandDto brandDto);

    /**
     * Request 转命令
     *
     * @param brandRequest Request
     * @return 命令
     */
    BrandCmd toCmd(BrandRequest brandRequest);

    /**
     * DTO 列表转 Response 列表
     *
     * @param brandDtoList DTO 列表
     * @return Response 列表
     */
    List<BrandResponse> fromDtoList(List<BrandDto> brandDtoList);

}
