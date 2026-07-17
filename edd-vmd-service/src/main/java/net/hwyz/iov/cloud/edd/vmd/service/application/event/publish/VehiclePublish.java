package net.hwyz.iov.cloud.edd.vmd.service.application.event.publish;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.EventReplayMetadata;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.VehicleEolEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.VehicleEolPartBoundEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.VehicleProduceEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.VehicleTolEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

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
     * @param vin     车架号
     * @param batchNum 批次号
     */
    public void produce(String vin, String batchNum) {
        log.info("发布车辆[{}]生产事件, batchNum={}", vin, batchNum);
        ctx.publishEvent(new VehicleProduceEvent(vin, batchNum));
    }

    /**
     * 车辆生产（补发）
     * <p>
     * VMD-DSN-CR-039: 车辆导入成功事件人工补发
     *
     * @param vin           车架号
     * @param batchNum      批次号
     * @param replayMetadata 补发元数据
     */
    public void produce(String vin, String batchNum, EventReplayMetadata replayMetadata) {
        log.info("补发车辆[{}]生产事件, batchNum={}, replayId={}", vin, batchNum, replayMetadata.getReplayId());
        ctx.publishEvent(new VehicleProduceEvent(vin, batchNum, replayMetadata));
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

    /**
     * 发布车辆总装上线事件
     *
     * @param vin 车架号
     * @param tolTime 总装上线时间
     */
    public void tol(String vin, Instant tolTime) {
        ctx.publishEvent(new VehicleTolEvent(vin, tolTime));
    }

    /**
     * 车辆下线零件绑定完成
     *
     * @param vin   车架号
     * @param parts 零件元数据列表
     */
    public void eolPartBound(String vin, List<VehicleEolPartBoundEvent.PartMeta> parts) {
        log.info("发布车辆[{}]下线零件绑定事件，零件数[{}]", vin, parts.size());
        ctx.publishEvent(new VehicleEolPartBoundEvent(vin, parts));
    }

}
