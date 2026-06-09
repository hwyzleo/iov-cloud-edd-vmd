package net.hwyz.iov.cloud.edd.vmd.service.adapter.web.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.request.PlantRequest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.vo.response.PlantResponse;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.PlantCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.PlantQuery;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.PlantDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台生产工厂 VO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface MptPlantAssembler {

    MptPlantAssembler INSTANCE = Mappers.getMapper(MptPlantAssembler.class);

    /**
     * DTO 转 VO
     *
     * @param plantDto DTO
     * @return VO
     */
    PlantResponse fromDto(PlantDto plantDto);

    /**
     * VO 转 DTO
     *
     * @param plantVo VO
     * @return DTO
     */
    PlantDto toDto(PlantRequest plantVo);

    /**
     * VO 转 CMD
     *
     * @param plantVo VO
     * @return CMD
     */
    PlantCmd toCmd(PlantRequest plantVo);

    /**
     * VO 转 Query
     *
     * @param plantVo VO
     * @return Query
     */
    PlantQuery toQuery(PlantRequest plantVo);

    /**
     * DTO 列表转 VO 列表
     *
     * @param plantDtoList DTO 列表
     * @return VO 列表
     */
    List<PlantResponse> fromDtoList(List<PlantDto> plantDtoList);

}
