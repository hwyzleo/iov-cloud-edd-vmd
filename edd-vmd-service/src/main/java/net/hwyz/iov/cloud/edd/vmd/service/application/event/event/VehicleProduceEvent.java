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

    /**
     * 批次号
     */
    private final String batchNum;

    /**
     * 事件补发元数据（可选，仅补发时携带）
     */
    private final EventReplayMetadata replayMetadata;

    public VehicleProduceEvent(String vin, String batchNum) {
        super(vin);
        this.vin = vin;
        this.batchNum = batchNum;
        this.replayMetadata = null;
    }

    public VehicleProduceEvent(String vin, String batchNum, EventReplayMetadata replayMetadata) {
        super(vin);
        this.vin = vin;
        this.batchNum = batchNum;
        this.replayMetadata = replayMetadata;
    }

}
