package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Configuration;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.ConfigurationHierarchy;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmConfigurationHierarchyPo;
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
     * @param mdmConfigurationPo PO
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

    /**
     * 产品树补全视图 PO 转领域值对象（CR-047 / US-031）
     *
     * @param mdmConfigurationHierarchyPo 产品树补全视图 PO
     * @return 配置产品树补全值对象
     */
    ConfigurationHierarchy toHierarchy(MdmConfigurationHierarchyPo mdmConfigurationHierarchyPo);

    /**
     * 产品树补全视图 PO 列表转领域值对象列表
     *
     * @param mdmConfigurationHierarchyPoList 产品树补全视图 PO 列表
     * @return 配置产品树补全值对象列表
     */
    List<ConfigurationHierarchy> toHierarchyList(List<MdmConfigurationHierarchyPo> mdmConfigurationHierarchyPoList);
}
