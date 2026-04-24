package net.hwyz.iov.cloud.edd.vmd.service.application.event.subscribe;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.enums.QrcodeType;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.QrcodeConfirmEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.QrcodeValidateEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.VehicleEolEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.VehicleProduceEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehicleLifecycleAppService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 车辆生命周期事件订阅类
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VehicleLifecycleSubscribe {

    private final VehicleAppService vehicleAppService;
    private final VehicleLifecycleAppService vehicleLifecycleAppService;

    /**
     * 订阅二维码扫描验证事件
     *
     * @param event 二维码确认事件
     */
    @EventListener
    public void onQrcodeValidateEvent(QrcodeValidateEvent event) {
        if (event.getType() == QrcodeType.VEHICLE_ACTIVE) {
            vehicleAppService.checkVehiclePresetOwner(event.getVin(), event.getAccountId());
        }
    }

    /**
     * 订阅二维码确认事件
     *
     * @param event 二维码确认事件
     */
    @EventListener
    public void onQrcodeConfirmEvent(QrcodeConfirmEvent event) {
        if (event.getType() == QrcodeType.VEHICLE_ACTIVE) {
            vehicleLifecycleAppService.recordVehicleActiveNode(event.getVin(), event.getAccountId());
        }
    }

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
        vehicleLifecycleAppService.recordEolNode(event.getVin(), event.getEolTime());
    }

}
