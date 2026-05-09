package net.hwyz.iov.cloud.edd.vmd.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.service.VmdVehicleModelConfigService;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VmdBuildConfigResponse;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 车辆车系车型配置相关服务降级处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class VmdVehicleModelConfigServiceFallbackFactory implements FallbackFactory<VmdVehicleModelConfigService> {

    @Override
    public VmdVehicleModelConfigService create(Throwable throwable) {
        return new VmdVehicleModelConfigService() {
            @Override
            public String getVehicleBuildConfigCode(Map<String, String> featureCodeMap) {
                log.error("车辆车系车型配置服务根据特征族特征值[{}]得到匹配的生产配置代码调用失败", featureCodeMap, throwable);
                return null;
            }

            @Override
            public List<VmdBuildConfigResponse> getBuildConfigListByBaseModelCode(String baseModelCode) {
                log.error("车辆车系车型配置服务根据基础车型代码[{}]获取生产配置列表调用失败", baseModelCode, throwable);
                return Collections.emptyList();
            }

            @Override
            public VmdBuildConfigResponse getBuildConfigByCode(String buildConfigCode) {
                log.error("车辆车系车型配置服务根据生产配置代码[{}]获取生产配置详细信息调用失败", buildConfigCode, throwable);
                return null;
            }
        };
    }
}
