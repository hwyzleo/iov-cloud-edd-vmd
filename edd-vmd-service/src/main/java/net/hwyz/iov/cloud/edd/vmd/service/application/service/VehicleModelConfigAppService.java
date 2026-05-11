package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBuildConfigRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleModelConfigAppService {

    private final VehBuildConfigRepository vehBuildConfigRepository;

    public String getVehicleBuildConfigCode(Map<String, String> featureCodeMap) {
        log.info("根据特征族特征值[{}]匹配生产配置代码", featureCodeMap);
        if (featureCodeMap == null || featureCodeMap.isEmpty()) {
            log.warn("特征族特征值为空，无法匹配生产配置代码");
            return null;
        }
        String buildConfigCode = vehBuildConfigRepository.selectBuildConfigCodeByFeatureCodeMap(featureCodeMap);
        log.info("匹配到的生产配置代码为[{}]", buildConfigCode);
        return buildConfigCode;
    }

}