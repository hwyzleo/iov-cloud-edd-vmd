package net.hwyz.iov.cloud.edd.vmd.service.application.event.event;

import lombok.Getter;

import java.util.Date;

/**
 * 车辆下线事件
 *
 * @author hwyz_leo
 */
@Getter
public class VehicleEolEvent extends BaseEvent {

    /**
     * 车架号
     */
    private final String vin;
    /**
     * 下线时间
     */
    private final Date eolTime;

    public VehicleEolEvent(String vin, Date eolTime) {
        super(vin);
        this.vin = vin;
        this.eolTime = eolTime;
    }

}
