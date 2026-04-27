package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.PlatformVo;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.PlatformDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台车辆平台 VO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface MptPlatformAssembler {

    MptPlatformAssembler INSTANCE = Mappers.getMapper(MptPlatformAssembler.class);

    /**
     * DTO 转 VO
     *
     * @param platformDto DTO
     * @return VO
     */
    PlatformVo fromDto(PlatformDto platformDto);

    /**
     * VO 转 DTO
     *
     * @param platformVo VO
     * @return DTO
     */
    PlatformDto toDto(PlatformVo platformVo);

    /**
     * DTO 列表转 VO 列表
     *
     * @param platformDtoList DTO 列表
     * @return VO 列表
     */
    List<PlatformVo> fromDtoList(List<PlatformDto> platformDtoList);

}
