package net.hwyz.iov.cloud.edd.vmd.service.application.event.event;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * MDM 工厂事件
 *
 * @author hwyz_leo
 */
@Getter
@Setter
public class MdmPlantEvent extends MdmEvent {

    /**
     * 工厂名称
     */
    private String name;

    public MdmPlantEvent(String eventType, String entityId, Long version, String code,
                         String name, LocalDateTime occurredAt) {
        super(eventType, entityId, version, code, occurredAt);
        this.name = name;
    }

}
