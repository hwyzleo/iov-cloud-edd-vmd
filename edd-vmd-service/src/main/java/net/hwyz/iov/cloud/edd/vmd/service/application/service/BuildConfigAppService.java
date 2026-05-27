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
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BuildConfig;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BuildConfigFeatureCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBuildConfigRepository;
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

    private final VehBuildConfigRepository vehBuildConfigRepository;
    private final VehBasicInfoRepository vehBasicInfoRepository;
    private final FeatureFamilyAppService featureFamilyAppService;

    public List<BuildConfigDto> search(BuildConfigQuery query) {
        Map<String, Object> map = new HashMap<>();
        map.put("platformCode", query.getPlatformCode());
        map.put("carLineCode", query.getCarLineCode());
        map.put("modelCode", query.getModelCode());
        map.put("baseModelCode", query.getBaseModelCode());
        map.put("code", query.getCode());
        map.put("name", ParamHelper.fuzzyQueryParam(query.getName()));
        map.put("beginTime", query.getBeginTime());
        map.put("endTime", query.getEndTime());
        List<BuildConfig> buildConfigList = vehBuildConfigRepository.selectByMap(map);
        return PageUtil.convert(buildConfigList, BuildConfigAssembler.INSTANCE::fromDomain);
    }

    public List<BuildConfigDto> getBuildConfigListByBaseModelCode(String baseModelCode) {
        List<BuildConfig> buildConfigList = vehBuildConfigRepository.selectByExample(BuildConfig.builder()
                .baseModelCode(baseModelCode)
                .enable(true)
                .build());
        return PageUtil.convert(buildConfigList, BuildConfigAssembler.INSTANCE::fromDomain);
    }

public BuildConfigDto getBuildConfigByCode(String code) {
        BuildConfig buildConfig = vehBuildConfigRepository.selectByCode(code);
        return BuildConfigAssembler.INSTANCE.fromDomain(buildConfig);
    }

    public BuildConfig getBuildConfigEntityByCode(String code) {
        return vehBuildConfigRepository.selectByCode(code);
    }

    public Boolean checkCodeUnique(Long buildConfigId, String code) {
        if (ObjUtil.isNull(buildConfigId)) {
            buildConfigId = -1L;
        }
        BuildConfig buildConfig = getBuildConfigEntityByCode(code);
        return !ObjUtil.isNotNull(buildConfig) || buildConfig.getId().longValue() == buildConfigId.longValue();
    }

    public Boolean checkBuildConfigVehicleExist(Long buildConfigId) {
        BuildConfig buildConfig = vehBuildConfigRepository.selectById(buildConfigId);
        Map<String, Object> map = new HashMap<>();
        map.put("buildConfigCode", buildConfig.getCode());
        return vehBasicInfoRepository.countByMap(map) > 0;
    }

    public BuildConfigDto getBuildConfigById(Long id) {
        return BuildConfigAssembler.INSTANCE.fromDomain(vehBuildConfigRepository.selectById(id));
    }

    public int createBuildConfig(BuildConfigCmd buildConfigCmd, String userId) {
        BuildConfig buildConfig = BuildConfigAssembler.INSTANCE.toDomain(buildConfigCmd);
        return vehBuildConfigRepository.insert(buildConfig);
    }

    public int modifyBuildConfig(BuildConfigCmd buildConfigCmd, String userId) {
        BuildConfig buildConfig = BuildConfigAssembler.INSTANCE.toDomain(buildConfigCmd);
        return vehBuildConfigRepository.update(buildConfig);
    }

    public int deleteBuildConfigByIds(Long[] ids) {
        return vehBuildConfigRepository.batchPhysicalDelete(ids);
    }

    public List<BuildConfigFeatureCodeDto> searchFeatureCode(String buildConfigCode, String familyCode) {
        BuildConfigFeatureCode example = BuildConfigFeatureCode.builder()
                .buildConfigCode(buildConfigCode)
                .familyCode(familyCode)
                .build();
        List<BuildConfigFeatureCode> list = vehBuildConfigRepository.selectFeatureCodeByExample(example);
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
        List<BuildConfigFeatureCode> list = vehBuildConfigRepository.selectFeatureCodeByExample(BuildConfigFeatureCode.builder().id(id).build());
        return list.isEmpty() ? null : BuildConfigFeatureCodeAssembler.INSTANCE.fromDomain(list.get(0));
    }

    public Boolean checkFeatureCodeUnique(Long id, String buildConfigCode, String familyCode) {
        if (ObjUtil.isNull(id)) {
            id = -1L;
        }
        List<BuildConfigFeatureCode> list = vehBuildConfigRepository.selectFeatureCodeByExample(BuildConfigFeatureCode.builder()
                .buildConfigCode(buildConfigCode)
                .familyCode(familyCode)
                .build());
        return list.isEmpty() || list.get(0).getId().longValue() == id.longValue();
    }

    public int createBuildConfigFeatureCode(BuildConfigFeatureCodeCmd featureCodeCmd) {
        BuildConfigFeatureCode featureCode = BuildConfigFeatureCodeAssembler.INSTANCE.toDomain(featureCodeCmd);
        return vehBuildConfigRepository.batchInsertFeatureCode(List.of(featureCode));
    }

    public int modifyBuildConfigFeatureCode(BuildConfigFeatureCodeCmd featureCodeCmd) {
        BuildConfigFeatureCode featureCode = BuildConfigFeatureCodeAssembler.INSTANCE.toDomain(featureCodeCmd);
        return vehBuildConfigRepository.updateFeatureCode(featureCode);
    }

    public int deleteBuildConfigFeatureCodeByIds(Long[] ids) {
        return vehBuildConfigRepository.batchPhysicalDeleteFeatureCode(ids);
    }

}