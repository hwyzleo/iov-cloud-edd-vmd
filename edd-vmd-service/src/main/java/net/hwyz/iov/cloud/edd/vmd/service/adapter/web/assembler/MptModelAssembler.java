package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.ModelVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.ModelDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台车型 VO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface MptModelAssembler {

    MptModelAssembler INSTANCE = Mappers.getMapper(MptModelAssembler.class);

    /**
     * DTO 转 VO
     *
     * @param modelDto DTO
     * @return VO
     */
    ModelVo fromDto(ModelDto modelDto);

    /**
     * VO 转 DTO
     *
     * @param modelVo VO
     * @return DTO
     */
    ModelDto toDto(ModelVo modelVo);

    /**
     * DTO 列表转 VO 列表
     *
     * @param modelDtoList DTO 列表
     * @return VO 列表
     */
    List<ModelVo> fromDtoList(List<ModelDto> modelDtoList);

}
