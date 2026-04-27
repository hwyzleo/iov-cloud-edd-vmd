package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.BuildConfigDto;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BuildConfig;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 生产配置 DTO 转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface BuildConfigAssembler {

    BuildConfigAssembler INSTANCE = Mappers.getMapper(BuildConfigAssembler.class);

    /**
     * 领域对象转 DTO
     *
     * @param buildConfig 领域对象
     * @return DTO
     */
    BuildConfigDto fromDomain(BuildConfig buildConfig);

    /**
     * DTO 转领域对象
     *
     * @param buildConfigDto DTO
     * @return 领域对象
     */
    BuildConfig toDomain(BuildConfigDto buildConfigDto);

    /**
     * 领域对象列表转 DTO 列表
     *
     * @param buildConfigList 领域对象列表
     * @return DTO 列表
     */
    List<BuildConfigDto> fromDomainList(List<BuildConfig> buildConfigList);

}
