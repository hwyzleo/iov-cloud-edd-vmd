package net.hwyz.iov.cloud.edd.vmd.service.application.event.subscribe;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.VehicleProduceEvent;
import net.hwyz.iov.cloud.tsp.sec.api.feign.service.ExSkService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleLifecycleAppService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 车辆密钥事件订阅类
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VehicleSkSubscribe {

    private final ExSkService exSkService;
    private final VehicleLifecycleAppService vehicleLifecycleAppService;

    /**
     * 订阅车辆生产事件
     *
     * @param event 车辆生产事件
     */
    @EventListener
    public void onVehicleProduceEvent(VehicleProduceEvent event) {
        Map<String, String> skMap = exSkService.generateVehicleSk(event.getVin());
        if (ObjUtil.isNotNull(skMap) && !skMap.isEmpty()) {
            vehicleLifecycleAppService.recordGenerateVehicleSkNode(event.getVin());
        }
    }

}
