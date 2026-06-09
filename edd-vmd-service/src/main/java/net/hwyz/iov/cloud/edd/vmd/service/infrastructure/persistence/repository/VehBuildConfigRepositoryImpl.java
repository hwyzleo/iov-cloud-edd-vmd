package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Configuration;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigurationFeatureCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehConfigurationRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.BuildConfigConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.BuildConfigFeatureCodeConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehBuildConfigMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehBuildConfigFeatureCodeMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehBuildConfigPo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehBuildConfigFeatureCodePo;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class VehBuildConfigRepositoryImpl implements VehConfigurationRepository {

    private final VehBuildConfigMapper vehBuildConfigMapper;
    private final VehBuildConfigFeatureCodeMapper vehBuildConfigFeatureCodeMapper;

    @Override
    public List<Configuration> selectByMap(Map<String, Object> map) {
        List<VehBuildConfigPo> poList = vehBuildConfigMapper.selectPoByMap(map);
        return PageUtil.convert(poList, BuildConfigConverter.INSTANCE::toDomain);
    }

    @Override
    public int countByMap(Map<String, Object> map) {
        return vehBuildConfigMapper.countPoByMap(map);
    }

    @Override
    public Configuration selectById(Long id) {
        return BuildConfigConverter.INSTANCE.toDomain(vehBuildConfigMapper.selectPoById(id));
    }

    @Override
    public Configuration selectByCode(String code) {
        return BuildConfigConverter.INSTANCE.toDomain(vehBuildConfigMapper.selectPoByCode(code));
    }

    @Override
    public int insert(Configuration configuration) {
        return vehBuildConfigMapper.insertPo(BuildConfigConverter.INSTANCE.fromDomain(configuration));
    }

    @Override
    public int update(Configuration configuration) {
        return vehBuildConfigMapper.updatePo(BuildConfigConverter.INSTANCE.fromDomain(configuration));
    }

    @Override
    public int batchPhysicalDelete(Long[] ids) {
        return vehBuildConfigMapper.batchPhysicalDeletePo(ids);
    }

    @Override
    public List<Configuration> selectByExample(Configuration example) {
        List<VehBuildConfigPo> poList = vehBuildConfigMapper.selectPoByExample(BuildConfigConverter.INSTANCE.fromDomain(example));
        return PageUtil.convert(poList, BuildConfigConverter.INSTANCE::toDomain);
    }

    @Override
    public List<ConfigurationFeatureCode> selectFeatureCodeByExample(ConfigurationFeatureCode example) {
        List<VehBuildConfigFeatureCodePo> poList = vehBuildConfigFeatureCodeMapper.selectPoByExample(BuildConfigFeatureCodeConverter.INSTANCE.fromDomain(example));
        return PageUtil.convert(poList, BuildConfigFeatureCodeConverter.INSTANCE::toDomain);
    }

    @Override
    public int batchInsertFeatureCode(List<ConfigurationFeatureCode> featureCodeList) {
        List<VehBuildConfigFeatureCodePo> poList = featureCodeList.stream()
                .map(BuildConfigFeatureCodeConverter.INSTANCE::fromDomain)
                .collect(Collectors.toList());
        return vehBuildConfigFeatureCodeMapper.batchInsertPo(poList);
    }

    @Override
    public int updateFeatureCode(ConfigurationFeatureCode featureCode) {
        return vehBuildConfigFeatureCodeMapper.updatePo(BuildConfigFeatureCodeConverter.INSTANCE.fromDomain(featureCode));
    }

    @Override
    public int batchPhysicalDeleteFeatureCode(Long[] ids) {
        return vehBuildConfigFeatureCodeMapper.batchPhysicalDeletePo(ids);
    }

    @Override
    public List<String> selectConfigurationCodeByFeatureCodeMap(Map<String, String> featureCodeMap) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("featureCodeMap", featureCodeMap);
        params.put("familyCount", featureCodeMap.size());
        return vehBuildConfigFeatureCodeMapper.selectBuildConfigCodeByFeatureCodeMap(params);
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