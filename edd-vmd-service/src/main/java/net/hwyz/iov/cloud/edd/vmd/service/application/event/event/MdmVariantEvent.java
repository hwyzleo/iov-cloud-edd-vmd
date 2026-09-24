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
     * 版本本地化名称
     */
    private String nameLocal;

    /**
     * 车型代码
     */
    private String modelCode;

    /**
     * 备注
     */
    private String description;

    public MdmVariantEvent(String eventType, String entityId, Long version, String code,
                           String name, String modelCode, LocalDateTime occurredAt) {
        super(eventType, entityId, version, code, occurredAt);
        this.name = name;
        this.modelCode = modelCode;
    }

}
