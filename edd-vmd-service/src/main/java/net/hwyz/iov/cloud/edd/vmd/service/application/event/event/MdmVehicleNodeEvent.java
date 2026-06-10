package net.hwyz.iov.cloud.edd.vmd.service.application.event.event;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * MDM 车载节点事件
 *
 * @author hwyz_leo
 */
@Getter
@Setter
public class MdmVehicleNodeEvent extends MdmEvent {

    /**
     * 车载节点名称
     */
    private String name;

    /**
     * 车载节点英文名称
     */
    private String nameEn;

    /**
     * 设备类型
     */
    private String type;

    /**
     * 设备项
     */
    private String deviceItem;

    /**
     * 功能域
     */
    private String funcDomain;

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * OTA支持类型
     */
    private String otaSupport;

    /**
     * 是否核心设备
     */
    private Boolean core;

    /**
     * 排序
     */
    private Integer sort;

    public MdmVehicleNodeEvent(String eventType, String entityId, Long version, String code,
                                String name, String nameEn, String type, String deviceItem,
                                String funcDomain, String nodeType, String otaSupport,
                                Boolean core, Integer sort, LocalDateTime occurredAt) {
        super(eventType, entityId, version, code, occurredAt);
        this.name = name;
        this.nameEn = nameEn;
        this.type = type;
        this.deviceItem = deviceItem;
        this.funcDomain = funcDomain;
        this.nodeType = nodeType;
        this.otaSupport = otaSupport;
        this.core = core;
        this.sort = sort;
    }

}
