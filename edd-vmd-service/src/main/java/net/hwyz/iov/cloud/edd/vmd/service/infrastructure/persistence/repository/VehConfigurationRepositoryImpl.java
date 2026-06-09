package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Configuration;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigurationOptionCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehConfigurationRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.ConfigurationConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.converter.ConfigurationOptionCodeConverter;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehConfigurationMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehConfigurationOptionCodeMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehConfigurationPo;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehConfigurationOptionCodePo;
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
    private final VehConfigurationOptionCodeMapper vehConfigurationOptionCodeMapper;

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
    public List<ConfigurationOptionCode> selectOptionCodeByExample(ConfigurationOptionCode example) {
        List<VehConfigurationOptionCodePo> poList = vehConfigurationOptionCodeMapper.selectPoByExample(ConfigurationOptionCodeConverter.INSTANCE.fromDomain(example));
        return PageUtil.convert(poList, ConfigurationOptionCodeConverter.INSTANCE::toDomain);
    }

    @Override
    public int batchInsertOptionCode(List<ConfigurationOptionCode> optionCodeList) {
        List<VehConfigurationOptionCodePo> poList = optionCodeList.stream()
                .map(ConfigurationOptionCodeConverter.INSTANCE::fromDomain)
                .collect(Collectors.toList());
        return vehConfigurationOptionCodeMapper.batchInsertPo(poList);
    }

    @Override
    public int updateOptionCode(ConfigurationOptionCode optionCode) {
        return vehConfigurationOptionCodeMapper.updatePo(ConfigurationOptionCodeConverter.INSTANCE.fromDomain(optionCode));
    }

    @Override
    public int batchPhysicalDeleteOptionCode(Long[] ids) {
        return vehConfigurationOptionCodeMapper.batchPhysicalDeletePo(ids);
    }

    @Override
    public List<String> selectConfigurationCodeByOptionCodeMap(Map<String, String> optionCodeMap) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("optionCodeMap", optionCodeMap);
        params.put("optionFamilyCount", optionCodeMap.size());
        return vehConfigurationOptionCodeMapper.selectConfigurationCodeByOptionCodeMap(params);
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
