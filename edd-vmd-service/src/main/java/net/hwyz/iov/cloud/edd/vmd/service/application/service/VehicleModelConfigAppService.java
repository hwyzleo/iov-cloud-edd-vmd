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
        return vehBuildConfigRepository.selectBuildConfigCodeByFeatureCodeMap(featureCodeMap);
    }

}