package net.hwyz.iov.cloud.edd.vmd.service.application.event.publish;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.VehicleEolEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.VehicleProduceEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 车辆事件发布类
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VehiclePublish {

    final ApplicationContext ctx;

    /**
     * 车辆生产
     *
     * @param vin 车架号
     */
    public void produce(String vin) {
        log.info("发布车辆[{}]生产事件", vin);
        ctx.publishEvent(new VehicleProduceEvent(vin));
    }

    /**
     * 车辆下线
     *
     * @param vin     车架号
     * @param eolTime 下线时间
     */
    public void eol(String vin, Instant eolTime) {
        log.info("发布车辆[{}]下线事件", vin);
        ctx.publishEvent(new VehicleEolEvent(vin, eolTime));
    }

}
