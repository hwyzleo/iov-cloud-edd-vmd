package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.ConfigurationAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.ConfigurationFeatureCodeAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigurationDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigurationFeatureCodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.OptionCodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.OptionFamilyDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.ConfigurationQuery;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Configuration;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigurationFeatureCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehConfigurationRepository;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ConfigurationCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ConfigurationFeatureCodeCmd;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigurationAppService {

    private final VehConfigurationRepository vehConfigurationRepository;
    private final VehBasicInfoRepository vehBasicInfoRepository;
    private final OptionFamilyAppService optionFamilyAppService;

    public List<ConfigurationDto> search(ConfigurationQuery query) {
        Map<String, Object> map = new HashMap<>();
        map.put("platformCode", query.getPlatformCode());
        map.put("carLineCode", query.getCarLineCode());
        map.put("modelCode", query.getModelCode());
        map.put("variantCode", query.getVariantCode());
        map.put("baseModelCode", query.getBaseModelCode());
        map.put("code", query.getCode());
        map.put("name", ParamHelper.fuzzyQueryParam(query.getName()));
        map.put("beginTime", query.getBeginTime());
        map.put("endTime", query.getEndTime());
        List<Configuration> configurationList = vehConfigurationRepository.selectByMap(map);
        return PageUtil.convert(configurationList, ConfigurationAssembler.INSTANCE::fromDomain);
    }

    public List<ConfigurationDto> getConfigurationListByVariantCode(String variantCode) {
        List<Configuration> configurationList = vehConfigurationRepository.selectByExample(Configuration.builder()
                .variantCode(variantCode)
                .enable(true)
                .build());
        return PageUtil.convert(configurationList, ConfigurationAssembler.INSTANCE::fromDomain);
    }

    @Deprecated
    public List<ConfigurationDto> getConfigurationListByBaseModelCode(String baseModelCode) {
        List<Configuration> configurationList = vehConfigurationRepository.selectByExample(Configuration.builder()
                .variantCode(baseModelCode)
                .enable(true)
                .build());
        return PageUtil.convert(configurationList, ConfigurationAssembler.INSTANCE::fromDomain);
    }

    public ConfigurationDto getConfigurationByCode(String code) {
        Configuration configuration = vehConfigurationRepository.selectByCode(code);
        return ConfigurationAssembler.INSTANCE.fromDomain(configuration);
    }

    public Configuration getConfigurationEntityByCode(String code) {
        return vehConfigurationRepository.selectByCode(code);
    }

    public Boolean checkCodeUnique(Long configurationId, String code) {
        if (ObjUtil.isNull(configurationId)) {
            configurationId = -1L;
        }
        Configuration configuration = getConfigurationEntityByCode(code);
        return !ObjUtil.isNotNull(configuration) || configuration.getId().longValue() == configurationId.longValue();
    }

    public Boolean checkConfigurationVehicleExist(Long configurationId) {
        Configuration configuration = vehConfigurationRepository.selectById(configurationId);
        Map<String, Object> map = new HashMap<>();
        map.put("configurationCode", configuration.getCode());
        return vehBasicInfoRepository.countByMap(map) > 0;
    }

    public ConfigurationDto getConfigurationById(Long id) {
        return ConfigurationAssembler.INSTANCE.fromDomain(vehConfigurationRepository.selectById(id));
    }

    public int createConfiguration(ConfigurationCmd configurationCmd, String userId) {
        Configuration configuration = ConfigurationAssembler.INSTANCE.toDomain(configurationCmd);
        return vehConfigurationRepository.insert(configuration);
    }

    public int modifyConfiguration(ConfigurationCmd configurationCmd, String userId) {
        Configuration configuration = ConfigurationAssembler.INSTANCE.toDomain(configurationCmd);
        return vehConfigurationRepository.update(configuration);
    }

    public int deleteConfigurationByIds(Long[] ids) {
        return vehConfigurationRepository.batchPhysicalDelete(ids);
    }

    public List<ConfigurationFeatureCodeDto> searchFeatureCode(String configurationCode, String familyCode) {
        ConfigurationFeatureCode example = ConfigurationFeatureCode.builder()
                .configurationCode(configurationCode)
                .familyCode(familyCode)
                .build();
        List<ConfigurationFeatureCode> list = vehConfigurationRepository.selectFeatureCodeByExample(example);
        List<ConfigurationFeatureCodeDto> dtoList = PageUtil.convert(list, ConfigurationFeatureCodeAssembler.INSTANCE::fromDomain);
        dtoList.forEach(dto -> {
            OptionFamilyDto optionFamily = optionFamilyAppService.getOptionFamilyByCode(dto.getFamilyCode());
            if (optionFamily != null) {
                dto.setFamilyName(optionFamily.getName());
            }
            if (dto.getFeatureCode() != null) {
                dto.setFeatureName(new String[dto.getFeatureCode().length]);
                int i = 0;
                for (String code : dto.getFeatureCode()) {
                    OptionCodeDto optionCode = optionFamilyAppService.getOptionCodeByCode(code);
                    if (optionCode != null) {
                        dto.getFeatureName()[i] = optionCode.getName();
                    }
                    i++;
                }
            }
        });
        return dtoList;
    }

    public ConfigurationFeatureCodeDto getConfigurationFeatureCodeById(Long id) {
        List<ConfigurationFeatureCode> list = vehConfigurationRepository.selectFeatureCodeByExample(ConfigurationFeatureCode.builder().id(id).build());
        return list.isEmpty() ? null : ConfigurationFeatureCodeAssembler.INSTANCE.fromDomain(list.get(0));
    }

    public Boolean checkFeatureCodeUnique(Long id, String configurationCode, String familyCode) {
        if (ObjUtil.isNull(id)) {
            id = -1L;
        }
        List<ConfigurationFeatureCode> list = vehConfigurationRepository.selectFeatureCodeByExample(ConfigurationFeatureCode.builder()
                .configurationCode(configurationCode)
                .familyCode(familyCode)
                .build());
        return list.isEmpty() || list.get(0).getId().longValue() == id.longValue();
    }

    public int createConfigurationFeatureCode(ConfigurationFeatureCodeCmd featureCodeCmd) {
        ConfigurationFeatureCode featureCode = ConfigurationFeatureCodeAssembler.INSTANCE.toDomain(featureCodeCmd);
        return vehConfigurationRepository.batchInsertFeatureCode(List.of(featureCode));
    }

    public int modifyConfigurationFeatureCode(ConfigurationFeatureCodeCmd featureCodeCmd) {
        ConfigurationFeatureCode featureCode = ConfigurationFeatureCodeAssembler.INSTANCE.toDomain(featureCodeCmd);
        return vehConfigurationRepository.updateFeatureCode(featureCode);
    }

    public int deleteConfigurationFeatureCodeByIds(Long[] ids) {
        return vehConfigurationRepository.batchPhysicalDeleteFeatureCode(ids);
    }

}
