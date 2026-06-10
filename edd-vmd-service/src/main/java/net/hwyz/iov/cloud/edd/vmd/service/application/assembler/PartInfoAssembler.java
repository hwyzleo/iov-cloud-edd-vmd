package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.PartInfoDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.PartInfoCmd;

import java.util.List;

/**
 * 物理零件实例 DTO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface PartInfoAssembler {

    PartInfoAssembler INSTANCE = Mappers.getMapper(PartInfoAssembler.class);

    /**
     * 领域对象转 DTO
     *
     * @param partInfo 领域对象
     * @return DTO
     */
    PartInfoDto fromDomain(PartInfo partInfo);

    /**
     * DTO 转领域对象
     *
     * @param partInfoDto DTO
     * @return 领域对象
     */
    PartInfo toDomain(PartInfoDto partInfoDto);

    /**
     * 命令转领域对象
     *
     * @param cmd 命令
     * @return 领域对象
     */
    PartInfo toDomain(PartInfoCmd cmd);

    /**
     * 领域对象列表转 DTO 列表
     *
     * @param partInfoList 领域对象列表
     * @return DTO 列表
     */
    List<PartInfoDto> fromDomainList(List<PartInfo> partInfoList);

}
