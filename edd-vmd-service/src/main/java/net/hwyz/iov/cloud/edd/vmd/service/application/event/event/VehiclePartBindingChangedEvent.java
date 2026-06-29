package net.hwyz.iov.cloud.edd.vmd.service.application.event.event;

import lombok.Getter;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.BindingChangeType;

import java.time.Instant;

/**
 * 车辆-零件绑定关系变更事件
 * <p>
 * 在 vehicle_part active 绑定发生变化时发布（TOL 首绑 / 换件换盒 / 解绑 / EOL 全时点）
 * 供下游（TSP 等）建立只读投影
 *
 * @author hwyz_leo
 */
@Getter
public class VehiclePartBindingChangedEvent extends BaseEvent {

    /**
     * 车架号
     */
    private final String vin;

    /**
     * 绑定ID（= vehicle_part.id）
     */
    private final Long bindingId;

    /**
     * 零件编码
     */
    private final String partCode;

    /**
     * 零件序列号
     */
    private final String sn;

    /**
     * 设备分类（取自 mdm_vehicle_node.device_category）
     */
    private final String deviceCategory;

    /**
     * 车载节点代码
     */
    private final String vehicleNodeCode;

    /**
     * ICCID1（仅 deviceCategory=TBOX 时取自 part_info.extra，单卡 / 非 TBOX 为空）
     */
    private final String iccid1;

    /**
     * ICCID2（仅 deviceCategory=TBOX 时取自 part_info.extra，单卡 / 非 TBOX 为空）
     */
    private final String iccid2;

    /**
     * 变更类型：BIND / UNBIND / REPLACE
     */
    private final BindingChangeType changeType;

    /**
     * 被替换的绑定ID（REPLACE 时指向被替换绑定）
     */
    private final Long replaceOfBindingId;

    /**
     * 事件发生时间
     */
    private final Instant occurredAt;

    /**
     * 事件序（复用 vehicle_part 主键 id + bind_time / unbind_time + bind_state 组合表达）
     */
    private final Long seq;

    public VehiclePartBindingChangedEvent(String vin, Long bindingId, String partCode, String sn,
                                          String deviceCategory, String vehicleNodeCode,
                                          String iccid1, String iccid2,
                                          BindingChangeType changeType, Long replaceOfBindingId,
                                          Instant occurredAt, Long seq) {
        super(vin);
        this.vin = vin;
        this.bindingId = bindingId;
        this.partCode = partCode;
        this.sn = sn;
        this.deviceCategory = deviceCategory;
        this.vehicleNodeCode = vehicleNodeCode;
        this.iccid1 = iccid1;
        this.iccid2 = iccid2;
        this.changeType = changeType;
        this.replaceOfBindingId = replaceOfBindingId;
        this.occurredAt = occurredAt;
        this.seq = seq;
    }

}