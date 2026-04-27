package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.PlatformDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Platform;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 车辆平台 DTO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface PlatformAssembler {

    PlatformAssembler INSTANCE = Mappers.getMapper(PlatformAssembler.class);

    /**
     * 领域对象转 DTO
     *
     * @param platform 领域对象
     * @return DTO
     */
    PlatformDto fromDomain(Platform platform);

    /**
     * DTO 转领域对象
     *
     * @param platformDto DTO
     * @return 领域对象
     */
    Platform toDomain(PlatformDto platformDto);

    /**
     * 领域对象列表转 DTO 列表
     *
     * @param platformList 领域对象列表
     * @return DTO 列表
     */
    List<PlatformDto> fromDomainList(List<Platform> platformList);

}
