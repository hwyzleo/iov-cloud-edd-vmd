package net.hwyz.iov.cloud.edd.vmd.service.application.event.event;

import lombok.Getter;

import java.time.Instant;

/**
 * 车辆总装上线事件
 *
 * @author hwyz_leo
 */
@Getter
public class VehicleTolEvent extends BaseEvent {

    /**
     * 车架号
     */
    private final String vin;
    /**
     * 总装上线时间
     */
    private final Instant tolTime;

    public VehicleTolEvent(String vin, Instant tolTime) {
        super(vin);
        this.vin = vin;
        this.tolTime = tolTime;
    }

}
