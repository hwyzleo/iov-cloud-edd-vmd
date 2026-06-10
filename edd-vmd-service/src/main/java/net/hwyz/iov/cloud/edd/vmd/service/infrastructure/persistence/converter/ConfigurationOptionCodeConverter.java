package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigurationOptionCode;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.MdmConfigurationOptionCodePo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 配置选项值关系领域对象转换器（原BuildConfigFeatureCodeConverter→ConfigurationFeatureCodeConverter，CR-018重命名）
 *
 * @author hwyz_leo
 */
@Mapper
public interface ConfigurationOptionCodeConverter {

    ConfigurationOptionCodeConverter INSTANCE = Mappers.getMapper(ConfigurationOptionCodeConverter.class);

    /**
     * PO 转领域对象
     *
     * @param po PO
     * @return 领域对象
     */
    ConfigurationOptionCode toDomain(MdmConfigurationOptionCodePo po);

    /**
     * PO 列表转领域对象列表
     *
     * @param poList PO 列表
     * @return 领域对象列表
     */
    List<ConfigurationOptionCode> toDomainList(List<MdmConfigurationOptionCodePo> poList);

    /**
     * 领域对象转 PO
     *
     * @param domain 领域对象
     * @return PO
     */
    MdmConfigurationOptionCodePo fromDomain(ConfigurationOptionCode domain);
}
