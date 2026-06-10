package net.hwyz.iov.cloud.edd.vmd.service.application.event.event;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * MDM 零件事件
 *
 * @author hwyz_leo
 */
@Getter
@Setter
public class MdmPartEvent extends MdmEvent {

    /**
     * 零件名称
     */
    private String name;

    /**
     * 零件类型
     */
    private String partType;

    /**
     * 车载节点代码
     */
    private String vehicleNodeCode;

    /**
     * 供应商代码
     */
    private String supplierCode;

    /**
     * 是否软件零件
     */
    private Boolean isSoftware;

    /**
     * 是否支持FOTA升级
     */
    private Boolean fotaUpgradeable;

    /**
     * 是否精准追溯
     */
    private Boolean isAccuratelyTraced;

    /**
     * 零件状态
     */
    private String status;

    public MdmPartEvent(String eventType, String entityId, Long version, String code,
                        String name, String partType, String vehicleNodeCode,
                        String supplierCode, Boolean isSoftware, Boolean fotaUpgradeable,
                        Boolean isAccuratelyTraced, String status, LocalDateTime occurredAt) {
        super(eventType, entityId, version, code, occurredAt);
        this.name = name;
        this.partType = partType;
        this.vehicleNodeCode = vehicleNodeCode;
        this.supplierCode = supplierCode;
        this.isSoftware = isSoftware;
        this.fotaUpgradeable = fotaUpgradeable;
        this.isAccuratelyTraced = isAccuratelyTraced;
        this.status = status;
    }

}
