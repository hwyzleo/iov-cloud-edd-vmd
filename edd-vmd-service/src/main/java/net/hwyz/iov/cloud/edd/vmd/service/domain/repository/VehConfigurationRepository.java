package net.hwyz.iov.cloud.edd.vmd.service.domain.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Configuration;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigurationOptionCode;
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

    List<ConfigurationOptionCode> selectOptionCodeByExample(ConfigurationOptionCode example);

    int batchInsertOptionCode(List<ConfigurationOptionCode> optionCodeList);

    int updateOptionCode(ConfigurationOptionCode optionCode);

    int batchPhysicalDeleteOptionCode(Long[] ids);

    List<String> selectConfigurationCodeByOptionCodeMap(Map<String, String> optionCodeMap);

    Configuration selectByExternalRefId(String externalRefId);

    long countBySource(SourceType source);

    int updateById(Configuration configuration);

}
