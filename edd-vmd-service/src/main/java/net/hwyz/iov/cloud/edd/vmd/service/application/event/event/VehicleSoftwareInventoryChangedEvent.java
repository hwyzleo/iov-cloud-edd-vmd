package net.hwyz.iov.cloud.edd.vmd.service.application.event.event;

import lombok.Getter;

import java.time.Instant;

/**
 * 车辆软件实装清单变更事件
 * <p>
 * 在软件实装记录发生变化时发布（EOL/车端上报/OTA回写/售后刷写）
 * 供下游（OTA 等）建立只读投影
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

    public VehicleSoftwareInventoryChangedEvent(String vin, Long bindingId, Long partId,
                                                  String softwareTargetCode, String softwarePartNo,
                                                  String softwareVersion, String slot,
                                                  String changeType, String source,
                                                  Boolean isConfirmed, Long inventoryVersion,
                                                  Instant occurredAt) {
        super(vin);
        this.vin = vin;
        this.bindingId = bindingId;
        this.partId = partId;
        this.softwareTargetCode = softwareTargetCode;
        this.softwarePartNo = softwarePartNo;
        this.softwareVersion = softwareVersion;
        this.slot = slot;
        this.changeType = changeType;
        this.source = source;
        this.isConfirmed = isConfirmed;
        this.inventoryVersion = inventoryVersion;
        this.occurredAt = occurredAt;
    }
}
