package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.BrandVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.BrandDto;
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
     * DTO 转 VO
     *
     * @param brandDto DTO
     * @return VO
     */
    BrandVo fromDto(BrandDto brandDto);

    /**
     * VO 转 DTO
     *
     * @param brandVo VO
     * @return DTO
     */
    BrandDto toDto(BrandVo brandVo);

    /**
     * DTO 列表转 VO 列表
     *
     * @param brandDtoList DTO 列表
     * @return VO 列表
     */
    List<BrandVo> fromDtoList(List<BrandDto> brandDtoList);

}
