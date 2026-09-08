package net.hwyz.iov.cloud.edd.vmd.service.application.event.event;

import lombok.Getter;

import java.time.Instant;

/**
 * 车辆软件实装清单变更事件
 * <p>
 * 在软件实装记录发生变化时发布（EOL/车端上报/OTA回写/售后刷写/OTA观测）
 * 供下游（OTA 等）建立只读投影
 * <p>
 * VMD-DSN-CR-046: 经 vmd_outbox 发布（Key=VIN），payload 完整携带
 * Target/Slot/active/digest，不对 MULTI_TARGET 降级。
 *
 * @author hwyz_leo
 */
@Getter
public class VehicleSoftwareInventoryChangedEvent extends BaseEvent {

    /**
     * 车架号
     */
    private final String vin;

    /**
     * 绑定ID（= vehicle_part.id）
     */
    private final Long bindingId;

    /**
     * 零件ID（= part_info.id）
     */
    private final Long partId;

    /**
     * 零件编码（= part_info.part_code）
     */
    private final String partCode;

    /**
     * 零件序列号（= part_info.sn）
     */
    private final String sn;

    /**
     * 车载节点代码（= vehicle_part.vehicle_node_code）
     */
    private final String vehicleNodeCode;

    /**
     * 软件目标代码
     */
    private final String softwareTargetCode;

    /**
     * 软件零件号
     */
    private final String softwarePartNo;

    /**
     * 软件版本
     */
    private final String softwareVersion;

    /**
     * 槽位（可空）
     */
    private final String slot;

    /**
     * 是否当前启动槽（同一 Target 多 Slot 的 active/standby）
     */
    private final Boolean active;

    /**
     * 实装制品摘要（可空）
     */
    private final String digest;

    /**
     * 变更类型（INITIAL/UPGRADE/ROLLBACK/REFLASH/REPAIR）
     */
    private final String changeType;

    /**
     * 来源（EOL/VEHICLE_REPORT/OTA/AFTER_SALES/MANUAL）
     */
    private final String source;

    /**
     * 是否已确认（confirmed/provisional）
     */
    private final Boolean isConfirmed;

    /**
     * 软件清单版本（单调递增）
     */
    private final Long inventoryVersion;

    /**
     * 事件发生时间
     */
    private final Instant occurredAt;

    /**
     * 全量构造（CR-046）
     */
    public VehicleSoftwareInventoryChangedEvent(String vin, Long bindingId, Long partId,
                                                  String partCode, String sn, String vehicleNodeCode,
                                                  String softwareTargetCode, String softwarePartNo,
                                                  String softwareVersion, String slot, Boolean active,
                                                  String digest, String changeType, String source,
                                                  Boolean isConfirmed, Long inventoryVersion,
                                                  Instant occurredAt) {
        super(vin);
        this.vin = vin;
        this.bindingId = bindingId;
        this.partId = partId;
        this.partCode = partCode;
        this.sn = sn;
        this.vehicleNodeCode = vehicleNodeCode;
        this.softwareTargetCode = softwareTargetCode;
        this.softwarePartNo = softwarePartNo;
        this.softwareVersion = softwareVersion;
        this.slot = slot;
        this.active = active;
        this.digest = digest;
        this.changeType = changeType;
        this.source = source;
        this.isConfirmed = isConfirmed;
        this.inventoryVersion = inventoryVersion;
        this.occurredAt = occurredAt;
    }

    /**
     * 兼容构造（无 partCode/sn/vehicleNodeCode/active/digest，供既有调用方）
     */
    public VehicleSoftwareInventoryChangedEvent(String vin, Long bindingId, Long partId,
                                                  String softwareTargetCode, String softwarePartNo,
                                                  String softwareVersion, String slot,
                                                  String changeType, String source,
                                                  Boolean isConfirmed, Long inventoryVersion,
                                                  Instant occurredAt) {
        this(vin, bindingId, partId, null, null, null,
                softwareTargetCode, softwarePartNo, softwareVersion, slot, Boolean.TRUE,
                null, changeType, source, isConfirmed, inventoryVersion, occurredAt);
    }
}
