package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Configuration;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigurationFeatureCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;

import java.util.List;
import java.util.Map;

public interface VehConfigurationRepository {

    List<Configuration> selectByMap(Map<String, Object> map);

    int countByMap(Map<String, Object> map);

    Configuration selectById(Long id);

    Configuration selectByCode(String code);

    int insert(Configuration configuration);

    int update(Configuration configuration);

    int batchPhysicalDelete(Long[] ids);

    List<Configuration> selectByExample(Configuration example);

    List<ConfigurationFeatureCode> selectFeatureCodeByExample(ConfigurationFeatureCode example);

    int batchInsertFeatureCode(List<ConfigurationFeatureCode> featureCodeList);

    int updateFeatureCode(ConfigurationFeatureCode featureCode);

    int batchPhysicalDeleteFeatureCode(Long[] ids);

    List<String> selectConfigurationCodeByFeatureCodeMap(Map<String, String> featureCodeMap);

    Configuration selectByExternalRefId(String externalRefId);

    long countBySource(SourceType source);

    int updateById(Configuration configuration);

}