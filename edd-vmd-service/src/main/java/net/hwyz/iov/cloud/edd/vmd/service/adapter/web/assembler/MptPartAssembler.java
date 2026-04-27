package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.PartVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.PartDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台零件 VO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface MptPartAssembler {

    MptPartAssembler INSTANCE = Mappers.getMapper(MptPartAssembler.class);

    /**
     * DTO 转 VO
     *
     * @param partDto DTO
     * @return VO
     */
    PartVo fromDto(PartDto partDto);

    /**
     * VO 转 DTO
     *
     * @param partVo VO
     * @return DTO
     */
    PartDto toDto(PartVo partVo);

    /**
     * DTO 列表转 VO 列表
     *
     * @param partDtoList DTO 列表
     * @return VO 列表
     */
    List<PartVo> fromDtoList(List<PartDto> partDtoList);

}
