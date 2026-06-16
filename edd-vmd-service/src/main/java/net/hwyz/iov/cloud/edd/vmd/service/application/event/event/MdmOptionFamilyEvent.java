package net.hwyz.iov.cloud.edd.vmd.service.application.event.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * MDM 选项族事件
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@NoArgsConstructor
public class MdmOptionFamilyEvent extends MdmEvent {

    /**
     * 选项族名称
     */
    private String name;

    /**
     * 本地名称
     */
    private String nameLocal;

    /**
     * 类型
     */
    private String type;

    public MdmOptionFamilyEvent(String eventType, String entityId, Long version, String code,
                                 String name, String nameLocal, String type, LocalDateTime occurredAt) {
        super(eventType, entityId, version, code, occurredAt);
        this.name = name;
        this.nameLocal = nameLocal;
        this.type = type;
    }

}
