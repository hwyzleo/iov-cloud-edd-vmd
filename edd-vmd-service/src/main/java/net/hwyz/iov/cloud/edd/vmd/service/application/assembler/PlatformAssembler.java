package net.hwyz.iov.cloud.edd.vmd.service.application.assembler;

import net.hwyz.iov.cloud.edd.vmd.api.vo.PlatformVo;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Platform;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台平台转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface PlatformAssembler {

    PlatformAssembler INSTANCE = Mappers.getMapper(PlatformAssembler.class);

    /**
     * 领域对象转数据传输对象
     *
     * @param platform 领域对象
     * @return 数据传输对象
     */
    PlatformVo fromDomain(Platform platform);

    /**
     * 数据传输对象转领域对象
     *
     * @param platformVo 数据传输对象
     * @return 领域对象
     */
    Platform toDomain(PlatformVo platformVo);

    /**
     * 领域对象列表转数据传输对象列表
     *
     * @param platformList 领域对象列表
     * @return 数据传输对象列表
     */
    List<PlatformVo> fromDomainList(List<Platform> platformList);

}
