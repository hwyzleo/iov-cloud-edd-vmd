package net.hwyz.iov.cloud.edd.vmd.service.application.event.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * MDM 版本事件
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@NoArgsConstructor
public class MdmVariantEvent extends MdmEvent {

    /**
     * 版本名称
     */
    private String name;

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

    public MdmVariantEvent(String eventType, String entityId, Long version, String code,
                           String name, String platformCode, String carLineCode, String modelCode, LocalDateTime occurredAt) {
        super(eventType, entityId, version, code, occurredAt);
        this.name = name;
        this.platformCode = platformCode;
        this.carLineCode = carLineCode;
        this.modelCode = modelCode;
    }

}
