package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Configuration;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehBuildConfigPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 生产配置领域对象转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface BuildConfigConverter {

    BuildConfigConverter INSTANCE = Mappers.getMapper(BuildConfigConverter.class);

    /**
     * PO 转领域对象
     *
     * @param vehBuildConfigPo PO
     * @return 领域对象
     */
    Configuration toDomain(VehBuildConfigPo vehBuildConfigPo);

    /**
     * PO 列表转领域对象列表
     *
     * @param vehBuildConfigPoList PO 列表
     * @return 领域对象列表
     */
    List<Configuration> toDomainList(List<VehBuildConfigPo> vehBuildConfigPoList);

    /**
     * 领域对象转 PO
     *
     * @param configuration 领域对象
     * @return PO
     */
    VehBuildConfigPo fromDomain(Configuration configuration);
}
