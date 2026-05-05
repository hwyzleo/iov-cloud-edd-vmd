package net.hwyz.iov.cloud.edd.vmd.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.service.VmdVehicleModelConfigService;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VmdBuildConfigResponse;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

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
            public String getVehicleBuildConfigCode(String baseModelCode, String exteriorCode, String interiorCode,
                                                    String wheelCode, String tireCode, String spareTireCode,
                                                    String adasCode, String seatCode) {
                log.error("车辆车系车型配置服务根据生产配置类型[{}:{}:{}:{}:{}:{}:{}:{}]得到匹配的车型配置代码调用失败", baseModelCode,
                        exteriorCode, interiorCode, wheelCode, tireCode, spareTireCode, adasCode, seatCode, throwable);
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
