package net.hwyz.iov.cloud.edd.vmd.service.application.event.event;

import lombok.Getter;

/**
 * 车辆生产事件
 *
 * @author hwyz_leo
 */
@Getter
public class VehicleProduceEvent extends BaseEvent {

    /**
     * 车架号
     */
    private final String vin;

    public VehicleProduceEvent(String vin) {
        super(vin);
        this.vin = vin;
    }

}
