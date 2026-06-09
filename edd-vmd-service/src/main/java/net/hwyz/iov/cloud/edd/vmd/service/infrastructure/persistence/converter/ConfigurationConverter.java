package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Configuration;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehConfigurationPo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 生产配置领域对象转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface ConfigurationConverter {

    ConfigurationConverter INSTANCE = Mappers.getMapper(ConfigurationConverter.class);

    /**
     * PO 转领域对象
     *
     * @param vehConfigurationPo PO
     * @return 领域对象
     */
    Configuration toDomain(VehConfigurationPo vehConfigurationPo);

    /**
     * PO 列表转领域对象列表
     *
     * @param vehConfigurationPoList PO 列表
     * @return 领域对象列表
     */
    List<Configuration> toDomainList(List<VehConfigurationPo> vehConfigurationPoList);

    /**
     * 领域对象转 PO
     *
     * @param configuration 领域对象
     * @return PO
     */
    VehConfigurationPo fromDomain(Configuration configuration);
}
