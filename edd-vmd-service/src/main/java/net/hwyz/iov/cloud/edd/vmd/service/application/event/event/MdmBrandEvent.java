package net.hwyz.iov.cloud.edd.vmd.service.application.event.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * MDM 品牌事件
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@NoArgsConstructor
public class MdmBrandEvent extends MdmEvent {

    /**
     * 品牌名称
     */
    private String name;

    public MdmBrandEvent(String eventType, String entityId, Long version, String code,
                         String name, LocalDateTime occurredAt) {
        super(eventType, entityId, version, code, occurredAt);
        this.name = name;
    }

}
