package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Configuration;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmConfigurationPo;
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
    Configuration toDomain(MdmConfigurationPo mdmConfigurationPo);

    /**
     * PO 列表转领域对象列表
     *
     * @param mdmConfigurationPoList PO 列表
     * @return 领域对象列表
     */
    List<Configuration> toDomainList(List<MdmConfigurationPo> mdmConfigurationPoList);

    /**
     * 领域对象转 PO
     *
     * @param configuration 领域对象
     * @return PO
     */
    MdmConfigurationPo fromDomain(Configuration configuration);
}
