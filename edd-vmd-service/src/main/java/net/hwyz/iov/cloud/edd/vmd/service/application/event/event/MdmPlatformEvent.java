package net.hwyz.iov.cloud.edd.vmd.service.application.event.event;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * MDM 平台事件
 *
 * @author hwyz_leo
 */
@Getter
@Setter
public class MdmPlatformEvent extends MdmEvent {

    /**
     * 平台名称
     */
    private String name;

    public MdmPlatformEvent(String eventType, String entityId, Long version, String code,
                            String name, LocalDateTime occurredAt) {
        super(eventType, entityId, version, code, occurredAt);
        this.name = name;
    }

}
