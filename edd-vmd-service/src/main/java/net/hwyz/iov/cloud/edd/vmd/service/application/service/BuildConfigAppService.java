package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.BuildConfigAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.assembler.BuildConfigFeatureCodeAssembler;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BuildConfigDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.BuildConfigFeatureCodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.FeatureCodeDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.FeatureFamilyDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.BuildConfigQuery;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Configuration;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigurationFeatureCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehConfigurationRepository;
import net.hwyz.iov.cloud.framework.common.util.ParamHelper;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import org.springframework.stereotype.Service;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.BuildConfigCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.BuildConfigFeatureCodeCmd;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BuildConfigAppService {

    private final VehConfigurationRepository vehConfigurationRepository;
    private final VehBasicInfoRepository vehBasicInfoRepository;
    private final FeatureFamilyAppService featureFamilyAppService;

    public List<BuildConfigDto> search(BuildConfigQuery query) {
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
        return PageUtil.convert(configurationList, BuildConfigAssembler.INSTANCE::fromDomain);
    }

    public List<BuildConfigDto> getBuildConfigListByVariantCode(String variantCode) {
        List<Configuration> configurationList = vehConfigurationRepository.selectByExample(Configuration.builder()
                .variantCode(variantCode)
                .enable(true)
                .build());
        return PageUtil.convert(configurationList, BuildConfigAssembler.INSTANCE::fromDomain);
    }

    @Deprecated
    public List<BuildConfigDto> getBuildConfigListByBaseModelCode(String baseModelCode) {
        List<Configuration> configurationList = vehConfigurationRepository.selectByExample(Configuration.builder()
                .variantCode(baseModelCode)
                .enable(true)
                .build());
        return PageUtil.convert(configurationList, BuildConfigAssembler.INSTANCE::fromDomain);
    }

    public BuildConfigDto getBuildConfigByCode(String code) {
        Configuration configuration = vehConfigurationRepository.selectByCode(code);
        return BuildConfigAssembler.INSTANCE.fromDomain(configuration);
    }

    public Configuration getBuildConfigEntityByCode(String code) {
        return vehConfigurationRepository.selectByCode(code);
    }

    public Boolean checkCodeUnique(Long buildConfigId, String code) {
        if (ObjUtil.isNull(buildConfigId)) {
            buildConfigId = -1L;
        }
        Configuration configuration = getBuildConfigEntityByCode(code);
        return !ObjUtil.isNotNull(configuration) || configuration.getId().longValue() == buildConfigId.longValue();
    }

    public Boolean checkBuildConfigVehicleExist(Long buildConfigId) {
        Configuration configuration = vehConfigurationRepository.selectById(buildConfigId);
        Map<String, Object> map = new HashMap<>();
        map.put("buildConfigCode", configuration.getCode());
        return vehBasicInfoRepository.countByMap(map) > 0;
    }

    public BuildConfigDto getBuildConfigById(Long id) {
        return BuildConfigAssembler.INSTANCE.fromDomain(vehConfigurationRepository.selectById(id));
    }

    public int createBuildConfig(BuildConfigCmd buildConfigCmd, String userId) {
        Configuration configuration = BuildConfigAssembler.INSTANCE.toDomain(buildConfigCmd);
        return vehConfigurationRepository.insert(configuration);
    }

    public int modifyBuildConfig(BuildConfigCmd buildConfigCmd, String userId) {
        Configuration configuration = BuildConfigAssembler.INSTANCE.toDomain(buildConfigCmd);
        return vehConfigurationRepository.update(configuration);
    }

    public int deleteBuildConfigByIds(Long[] ids) {
        return vehConfigurationRepository.batchPhysicalDelete(ids);
    }

    public List<BuildConfigFeatureCodeDto> searchFeatureCode(String buildConfigCode, String familyCode) {
        ConfigurationFeatureCode example = ConfigurationFeatureCode.builder()
                .configurationCode(buildConfigCode)
                .familyCode(familyCode)
                .build();
        List<ConfigurationFeatureCode> list = vehConfigurationRepository.selectFeatureCodeByExample(example);
        List<BuildConfigFeatureCodeDto> dtoList = PageUtil.convert(list, BuildConfigFeatureCodeAssembler.INSTANCE::fromDomain);
        dtoList.forEach(dto -> {
            FeatureFamilyDto featureFamily = featureFamilyAppService.getFeatureFamilyByCode(dto.getFamilyCode());
            if (featureFamily != null) {
                dto.setFamilyName(featureFamily.getName());
            }
            if (dto.getFeatureCode() != null) {
                dto.setFeatureName(new String[dto.getFeatureCode().length]);
                int i = 0;
                for (String code : dto.getFeatureCode()) {
                    FeatureCodeDto featureCode = featureFamilyAppService.getFeatureCodeByCode(code);
                    if (featureCode != null) {
                        dto.getFeatureName()[i] = featureCode.getName();
                    }
                    i++;
                }
            }
        });
        return dtoList;
    }

    public BuildConfigFeatureCodeDto getBuildConfigFeatureCodeById(Long id) {
        List<ConfigurationFeatureCode> list = vehConfigurationRepository.selectFeatureCodeByExample(ConfigurationFeatureCode.builder().id(id).build());
        return list.isEmpty() ? null : BuildConfigFeatureCodeAssembler.INSTANCE.fromDomain(list.get(0));
    }

    public Boolean checkFeatureCodeUnique(Long id, String buildConfigCode, String familyCode) {
        if (ObjUtil.isNull(id)) {
            id = -1L;
        }
        List<ConfigurationFeatureCode> list = vehConfigurationRepository.selectFeatureCodeByExample(ConfigurationFeatureCode.builder()
                .configurationCode(buildConfigCode)
                .familyCode(familyCode)
                .build());
        return list.isEmpty() || list.get(0).getId().longValue() == id.longValue();
    }

    public int createBuildConfigFeatureCode(BuildConfigFeatureCodeCmd featureCodeCmd) {
        ConfigurationFeatureCode featureCode = BuildConfigFeatureCodeAssembler.INSTANCE.toDomain(featureCodeCmd);
        return vehConfigurationRepository.batchInsertFeatureCode(List.of(featureCode));
    }

    public int modifyBuildConfigFeatureCode(BuildConfigFeatureCodeCmd featureCodeCmd) {
        ConfigurationFeatureCode featureCode = BuildConfigFeatureCodeAssembler.INSTANCE.toDomain(featureCodeCmd);
        return vehConfigurationRepository.updateFeatureCode(featureCode);
    }

    public int deleteBuildConfigFeatureCodeByIds(Long[] ids) {
        return vehConfigurationRepository.batchPhysicalDeleteFeatureCode(ids);
    }

}