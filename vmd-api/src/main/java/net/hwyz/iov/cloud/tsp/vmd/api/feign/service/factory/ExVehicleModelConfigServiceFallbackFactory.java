package net.hwyz.iov.cloud.tsp.vmd.api.feign.service.factory;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.tsp.vmd.api.feign.service.ExVehicleModelConfigService;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 车辆车系车型配置相关服务降级处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class ExVehicleModelConfigServiceFallbackFactory implements FallbackFactory<ExVehicleModelConfigService> {

    @Override
    public ExVehicleModelConfigService create(Throwable throwable) {
        return new ExVehicleModelConfigService() {
            @Override
            public String getVehicleBuildConfigCode(String baseModelCode, String exteriorCode, String interiorCode,
                                                    String wheelCode, String tireCode, String spareTireCode,
                                                    String adasCode, String seatCode) {
                logger.error("车辆车系车型配置服务根据生产配置类型[{}:{}:{}:{}:{}:{}:{}:{}]得到匹配的车型配置代码调用失败", baseModelCode,
                        exteriorCode, interiorCode, wheelCode, tireCode, spareTireCode, adasCode, seatCode, throwable);
                return null;
            }
        };
    }
}
