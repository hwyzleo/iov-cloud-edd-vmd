package net.hwyz.iov.cloud.edd.vmd.service.application.event.event;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * MDM 配置事件
 *
 * @author hwyz_leo
 */
@Getter
@Setter
public class MdmConfigurationEvent extends MdmEvent {

    /**
     * 配置名称
     */
    private String name;

    /**
     * 英文名称
     */
    private String nameEn;

    /**
     * 平台代码
     */
    private String platformCode;

    /**
     * 车系代码
     */
    private String carLineCode;

    /**
     * 车型代码
     */
    private String modelCode;

    /**
     * 版本代码
     */
    private String variantCode;

    /**
     * 阶段代码
     */
    private String vehicleStageCode;

    /**
     * 是否启用
     */
    private Boolean enable;

    /**
     * 排序
     */
    private Integer sort;

    public MdmConfigurationEvent(String eventType, String entityId, Long version, String code,
                                  String name, String nameEn, String platformCode, String carLineCode,
                                  String modelCode, String variantCode, String vehicleStageCode,
                                  Boolean enable, Integer sort, LocalDateTime occurredAt) {
        super(eventType, entityId, version, code, occurredAt);
        this.name = name;
        this.nameEn = nameEn;
        this.platformCode = platformCode;
        this.carLineCode = carLineCode;
        this.modelCode = modelCode;
        this.variantCode = variantCode;
        this.vehicleStageCode = vehicleStageCode;
        this.enable = enable;
        this.sort = sort;
    }

}
