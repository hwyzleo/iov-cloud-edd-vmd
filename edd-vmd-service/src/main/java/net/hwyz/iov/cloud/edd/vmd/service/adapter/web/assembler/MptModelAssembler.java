package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.ModelRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.ModelResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ModelCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ModelDto;
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
    ModelResponse fromDto(ModelDto modelDto);

    /**
     * VO 转 DTO
     *
     * @param modelVo VO
     * @return DTO
     */
    ModelDto toDto(ModelRequest modelVo);

    /**
     * VO 转 CMD
     *
     * @param modelVo VO
     * @return CMD
     */
    ModelCmd toCmd(ModelRequest modelVo);

    /**
     * DTO 列表转 VO 列表
     *
     * @param modelDtoList DTO 列表
     * @return VO 列表
     */
    List<ModelResponse> fromDtoList(List<ModelDto> modelDtoList);

}
