package net.hwyz.iov.cloud.edd.vmd.service.application.event.event;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * MDM 车系事件
 *
 * @author hwyz_leo
 */
@Getter
@Setter
public class MdmCarLineEvent extends MdmEvent {

    /**
     * 车系名称
     */
    private String name;

    /**
     * 品牌代码
     */
    private String brandCode;

    public MdmCarLineEvent(String eventType, String entityId, Long version, String code,
                           String name, String brandCode, LocalDateTime occurredAt) {
        super(eventType, entityId, version, code, occurredAt);
        this.name = name;
        this.brandCode = brandCode;
    }

}
