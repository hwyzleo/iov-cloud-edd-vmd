package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.BuildConfigVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.BuildConfigDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台生产配置 VO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface MptBuildConfigAssembler {

    MptBuildConfigAssembler INSTANCE = Mappers.getMapper(MptBuildConfigAssembler.class);

    /**
     * DTO 转 VO
     *
     * @param buildConfigDto DTO
     * @return VO
     */
    BuildConfigVo fromDto(BuildConfigDto buildConfigDto);

    /**
     * VO 转 DTO
     *
     * @param buildConfigVo VO
     * @return DTO
     */
    BuildConfigDto toDto(BuildConfigVo buildConfigVo);

    /**
     * DTO 列表转 VO 列表
     *
     * @param buildConfigDtoList DTO 列表
     * @return VO 列表
     */
    List<BuildConfigVo> fromDtoList(List<BuildConfigDto> buildConfigDtoList);

}
