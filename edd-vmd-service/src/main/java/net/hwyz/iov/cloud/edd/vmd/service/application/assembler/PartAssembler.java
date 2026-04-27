package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.PartDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Part;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.PartCmd;

import java.util.List;

/**
 * 零件 DTO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface PartAssembler {

    PartAssembler INSTANCE = Mappers.getMapper(PartAssembler.class);

    /**
     * 领域对象转 DTO
     *
     * @param part 领域对象
     * @return DTO
     */
    PartDto fromDomain(Part part);

    /**
     * DTO 转领域对象
     *
     * @param partDto DTO
     * @return 领域对象
     */
    Part toDomain(PartDto partDto);
    /**
     * 命令转领域对象
     *
     * @param cmd 命令
     * @return 领域对象
     */
    Part toDomain(PartCmd cmd);


    /**
     * 领域对象列表转 DTO 列表
     *
     * @param partList 领域对象列表
     * @return DTO 列表
     */
    List<PartDto> fromDomainList(List<Part> partList);

}
