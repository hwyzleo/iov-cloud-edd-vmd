package net.hwyz.iov.cloud.edd.vmd.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.service.VmdVehicleLifecycleService;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 车辆生命周期相关服务降级处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class VmdVehicleLifecycleServiceFallbackFactory implements FallbackFactory<VmdVehicleLifecycleService> {

    @Override
    public VmdVehicleLifecycleService create(Throwable throwable) {
        return new VmdVehicleLifecycleService() {
            @Override
            public void recordFirstApplyNode(String vin, String nodeCode) {
                log.error("车辆生命周期服务记录车辆[{}]第一次申请节点[{}]调用失败", vin, nodeCode, throwable);
            }
        };
    }
}
