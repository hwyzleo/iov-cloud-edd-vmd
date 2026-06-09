package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigurationFeatureCode;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehConfigurationFeatureCodePo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 生产配置特征码领域对象转换器
 *
 * @author hwyz_leo
 */
@Mapper
public interface ConfigurationFeatureCodeConverter {

    ConfigurationFeatureCodeConverter INSTANCE = Mappers.getMapper(ConfigurationFeatureCodeConverter.class);

    /**
     * PO 转领域对象
     *
     * @param po PO
     * @return 领域对象
     */
    ConfigurationFeatureCode toDomain(VehConfigurationFeatureCodePo po);

    /**
     * PO 列表转领域对象列表
     *
     * @param poList PO 列表
     * @return 领域对象列表
     */
    List<ConfigurationFeatureCode> toDomainList(List<VehConfigurationFeatureCodePo> poList);

    /**
     * 领域对象转 PO
     *
     * @param domain 领域对象
     * @return PO
     */
    VehConfigurationFeatureCodePo fromDomain(ConfigurationFeatureCode domain);
}
