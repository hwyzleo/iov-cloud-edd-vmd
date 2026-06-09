package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehConfigurationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleModelConfigAppService {

    private final VehConfigurationRepository vehConfigurationRepository;

    public String getVehicleBuildConfigCode(Map<String, String> featureCodeMap) {
        log.info("根据特征族特征值[{}]匹配生产配置代码", featureCodeMap);
        if (featureCodeMap == null || featureCodeMap.isEmpty()) {
            log.warn("特征族特征值为空，无法匹配生产配置代码");
            return null;
        }
        List<String> buildConfigCodes = vehConfigurationRepository.selectConfigurationCodeByFeatureCodeMap(featureCodeMap);
        if (buildConfigCodes == null || buildConfigCodes.isEmpty()) {
            log.warn("未匹配到任何生产配置代码，特征族特征值为[{}]", featureCodeMap);
            return null;
        }
        if (buildConfigCodes.size() > 1) {
            log.warn("特征族特征值[{}]匹配到多个生产配置代码[{}]，数据可能存在重复，取首条返回", featureCodeMap, buildConfigCodes);
        }
        String buildConfigCode = buildConfigCodes.get(0);
        log.info("匹配到的生产配置代码为[{}]", buildConfigCode);
        return buildConfigCode;
    }

}