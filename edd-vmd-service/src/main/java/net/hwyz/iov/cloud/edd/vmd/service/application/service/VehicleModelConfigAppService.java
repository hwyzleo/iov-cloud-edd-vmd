package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BuildConfig;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BuildConfigFeatureCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBuildConfigRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleModelConfigAppService {

    private final VehBuildConfigRepository vehBuildConfigRepository;

    public String getBuildConfigCodeByType(String baseModelCode, String exteriorCode, String interiorCode, String wheelCode,
                                           String tireCode, String spareTireCode, String adasCode, String seatCode) {
        Map<String, String> featureCodeMap = new HashMap<>();
        if (exteriorCode != null && !exteriorCode.isEmpty()) {
            featureCodeMap.put("EXTERIOR", exteriorCode);
        }
        if (interiorCode != null && !interiorCode.isEmpty()) {
            featureCodeMap.put("INTERIOR", interiorCode);
        }
        if (wheelCode != null && !wheelCode.isEmpty()) {
            featureCodeMap.put("WHEEL", wheelCode);
        }
        if (tireCode != null && !tireCode.isEmpty()) {
            featureCodeMap.put("TIRE", tireCode);
        }
        if (spareTireCode != null && !spareTireCode.isEmpty()) {
            featureCodeMap.put("SPARE_TIRE", spareTireCode);
        }
        if (adasCode != null && !adasCode.isEmpty()) {
            featureCodeMap.put("ADAS", adasCode);
        }
        if (seatCode != null && !seatCode.isEmpty()) {
            featureCodeMap.put("SEAT", seatCode);
        }

        List<BuildConfig> buildConfigList = vehBuildConfigRepository.selectByExample(BuildConfig.builder()
                .baseModelCode(baseModelCode)
                .build());

        for (BuildConfig buildConfig : buildConfigList) {
            List<BuildConfigFeatureCode> featureCodes = vehBuildConfigRepository.selectFeatureCodeByExample(
                    BuildConfigFeatureCode.builder()
                            .buildConfigCode(buildConfig.getCode())
                            .build());

            boolean matched = true;
            for (Map.Entry<String, String> entry : featureCodeMap.entrySet()) {
                String familyCode = entry.getKey();
                String expectedFeatureCode = entry.getValue();

                boolean found = false;
                for (BuildConfigFeatureCode fc : featureCodes) {
                    if (fc.getFamilyCode().equals(familyCode)) {
                        if (fc.getFeatureCode() != null && fc.getFeatureCode().contains(expectedFeatureCode)) {
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    matched = false;
                    break;
                }
            }

            if (matched) {
                if (buildConfigList.size() > 1) {
                    log.warn("车型[{}]特征值组合匹配到多个生产配置", baseModelCode);
                }
                return buildConfig.getCode();
            }
        }
        return null;
    }

    public String getVehicleBuildConfigCode(String baseModelCode, String exteriorCode, String interiorCode, String wheelCode,
                                            String tireCode, String spareTireCode, String adasCode, String seatCode) {
        return getBuildConfigCodeByType(baseModelCode, exteriorCode, interiorCode, wheelCode, tireCode, spareTireCode, adasCode, seatCode);
    }

}