package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.PartRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.PartResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.PartDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.PartCmd;

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
    PartResponse fromDto(PartDto partDto);

    /**
     * VO 转 DTO
     *
     * @param partVo VO
     * @return DTO
     */
    PartDto toDto(PartRequest partVo);
    /**
     * VO 转命令
     *
     * @param vo VO
     * @return 命令
     */
    PartCmd toCmd(PartRequest vo);


    /**
     * DTO 列表转 VO 列表
     *
     * @param partDtoList DTO 列表
     * @return VO 列表
     */
    List<PartResponse> fromDtoList(List<PartDto> partDtoList);

}
