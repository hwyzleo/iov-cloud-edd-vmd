package net.hwyz.iov.cloud.edd.vmd.service.application.event.subscribe;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.VehicleEolEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.VehicleProduceEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleLifecycleAppService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 车辆生命周期事件订阅类
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VehicleLifecycleSubscribe {

    private final VehicleLifecycleAppService vehicleLifecycleAppService;

    /**
     * 订阅车辆生产事件
     *
     * @param event 车辆生产事件
     */
    @EventListener
    public void onVehicleProduceEvent(VehicleProduceEvent event) {
        vehicleLifecycleAppService.recordProduceNode(event.getVin());
    }

    /**
     * 订阅车辆下线事件
     *
     * @param event 车辆下线事件
     */
    @EventListener
    public void onVehicleEolEvent(VehicleEolEvent event) {
        vehicleLifecycleAppService.recordEolNode(event.getVin(), Date.from(event.getEolTime()));
    }

}
