package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.OptionCodeRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.OptionFamilyRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.OptionCodeResponse;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.OptionFamilyResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.OptionCodeCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.OptionFamilyCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.OptionCodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.OptionFamilyDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台选装 VO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface MptOptionAssembler {

    MptOptionAssembler INSTANCE = Mappers.getMapper(MptOptionAssembler.class);

    /**
     * 选装族 DTO 转 VO
     *
     * @param dto 选装族 DTO
     * @return 选装族 VO
     */
    OptionFamilyResponse fromFamilyDto(OptionFamilyDto dto);

    /**
     * 选装族 DTO 列表转 VO 列表
     *
     * @param dtoList 选装族 DTO 列表
     * @return 选装族 VO 列表
     */
    List<OptionFamilyResponse> fromFamilyDtoList(List<OptionFamilyDto> dtoList);

    /**
     * 选装族请求 VO 转命令 DTO
     *
     * @param request 选装族请求 VO
     * @return 选装族命令 DTO
     */
    OptionFamilyCmd toFamilyCmd(OptionFamilyRequest request);

    /**
     * 选装值 DTO 转 VO
     *
     * @param dto 选装值 DTO
     * @return 选装值 VO
     */
    OptionCodeResponse fromCodeDto(OptionCodeDto dto);

    /**
     * 选装值 DTO 列表转 VO 列表
     *
     * @param dtoList 选装值 DTO 列表
     * @return 选装值 VO 列表
     */
    List<OptionCodeResponse> fromCodeDtoList(List<OptionCodeDto> dtoList);

    /**
     * 选装值请求 VO 转命令 DTO
     *
     * @param request 选装值请求 VO
     * @return 选装值命令 DTO
     */
    OptionCodeCmd toCodeCmd(OptionCodeRequest request);

}
