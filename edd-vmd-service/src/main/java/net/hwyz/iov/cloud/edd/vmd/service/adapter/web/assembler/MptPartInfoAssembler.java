package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.PartInfoRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.PartInfoResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.PartInfoDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.PartInfoCmd;

import java.util.List;

/**
 * 管理后台物理零件实例 VO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface MptPartInfoAssembler {

    MptPartInfoAssembler INSTANCE = Mappers.getMapper(MptPartInfoAssembler.class);

    /**
     * DTO 转 VO
     *
     * @param partInfoDto DTO
     * @return VO
     */
    PartInfoResponse fromDto(PartInfoDto partInfoDto);

    /**
     * VO 转 DTO
     *
     * @param partInfoVo VO
     * @return DTO
     */
    PartInfoDto toDto(PartInfoRequest partInfoVo);

    /**
     * VO 转命令
     *
     * @param vo VO
     * @return 命令
     */
    PartInfoCmd toCmd(PartInfoRequest vo);

    /**
     * DTO 列表转 VO 列表
     *
     * @param partInfoDtoList DTO 列表
     * @return VO 列表
     */
    List<PartInfoResponse> fromDtoList(List<PartInfoDto> partInfoDtoList);

}
