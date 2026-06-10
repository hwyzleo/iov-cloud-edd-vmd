package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Platform;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmPlatformPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 平台领域对象转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface PlatformConverter {

    PlatformConverter INSTANCE = Mappers.getMapper(PlatformConverter.class);

    /**
     * PO 转领域对象
     *
     * @param mdmPlatformPo PO
     * @return 领域对象
     */
    Platform toDomain(MdmPlatformPo mdmPlatformPo);

    /**
     * PO 列表转领域对象列表
     *
     * @param mdmPlatformPoList PO 列表
     * @return 领域对象列表
     */
    List<Platform> toDomainList(List<MdmPlatformPo> mdmPlatformPoList);

    /**
     * 领域对象转 PO
     *
     * @param platform 领域对象
     * @return PO
     */
    MdmPlatformPo fromDomain(Platform platform);
}
