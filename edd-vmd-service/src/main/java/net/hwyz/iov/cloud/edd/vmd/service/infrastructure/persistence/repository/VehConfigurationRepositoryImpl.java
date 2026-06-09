package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Configuration;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigurationFeatureCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehConfigurationRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.ConfigurationConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.ConfigurationFeatureCodeConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehConfigurationMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehConfigurationFeatureCodeMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehConfigurationPo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehConfigurationFeatureCodePo;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class VehConfigurationRepositoryImpl implements VehConfigurationRepository {

    private final VehConfigurationMapper vehConfigurationMapper;
    private final VehConfigurationFeatureCodeMapper vehConfigurationFeatureCodeMapper;

    @Override
    public List<Configuration> selectByMap(Map<String, Object> map) {
        List<VehConfigurationPo> poList = vehConfigurationMapper.selectPoByMap(map);
        return PageUtil.convert(poList, ConfigurationConverter.INSTANCE::toDomain);
    }

    @Override
    public int countByMap(Map<String, Object> map) {
        return vehConfigurationMapper.countPoByMap(map);
    }

    @Override
    public Configuration selectById(Long id) {
        return ConfigurationConverter.INSTANCE.toDomain(vehConfigurationMapper.selectPoById(id));
    }

    @Override
    public Configuration selectByCode(String code) {
        return ConfigurationConverter.INSTANCE.toDomain(vehConfigurationMapper.selectPoByCode(code));
    }

    @Override
    public int insert(Configuration configuration) {
        return vehConfigurationMapper.insertPo(ConfigurationConverter.INSTANCE.fromDomain(configuration));
    }

    @Override
    public int update(Configuration configuration) {
        return vehConfigurationMapper.updatePo(ConfigurationConverter.INSTANCE.fromDomain(configuration));
    }

    @Override
    public int batchPhysicalDelete(Long[] ids) {
        return vehConfigurationMapper.batchPhysicalDeletePo(ids);
    }

    @Override
    public List<Configuration> selectByExample(Configuration example) {
        List<VehConfigurationPo> poList = vehConfigurationMapper.selectPoByExample(ConfigurationConverter.INSTANCE.fromDomain(example));
        return PageUtil.convert(poList, ConfigurationConverter.INSTANCE::toDomain);
    }

    @Override
    public List<ConfigurationFeatureCode> selectFeatureCodeByExample(ConfigurationFeatureCode example) {
        List<VehConfigurationFeatureCodePo> poList = vehConfigurationFeatureCodeMapper.selectPoByExample(ConfigurationFeatureCodeConverter.INSTANCE.fromDomain(example));
        return PageUtil.convert(poList, ConfigurationFeatureCodeConverter.INSTANCE::toDomain);
    }

    @Override
    public int batchInsertFeatureCode(List<ConfigurationFeatureCode> featureCodeList) {
        List<VehConfigurationFeatureCodePo> poList = featureCodeList.stream()
                .map(ConfigurationFeatureCodeConverter.INSTANCE::fromDomain)
                .collect(Collectors.toList());
        return vehConfigurationFeatureCodeMapper.batchInsertPo(poList);
    }

    @Override
    public int updateFeatureCode(ConfigurationFeatureCode featureCode) {
        return vehConfigurationFeatureCodeMapper.updatePo(ConfigurationFeatureCodeConverter.INSTANCE.fromDomain(featureCode));
    }

    @Override
    public int batchPhysicalDeleteFeatureCode(Long[] ids) {
        return vehConfigurationFeatureCodeMapper.batchPhysicalDeletePo(ids);
    }

    @Override
    public List<String> selectConfigurationCodeByFeatureCodeMap(Map<String, String> featureCodeMap) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("featureCodeMap", featureCodeMap);
        params.put("familyCount", featureCodeMap.size());
        return vehConfigurationFeatureCodeMapper.selectConfigurationCodeByFeatureCodeMap(params);
    }

    @Override
    public Configuration selectByExternalRefId(String externalRefId) {
        return null;
    }

    @Override
    public long countBySource(SourceType source) {
        return 0;
    }

    @Override
    public int updateById(Configuration configuration) {
        return 0;
    }

}
