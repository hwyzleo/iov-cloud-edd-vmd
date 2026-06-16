package net.hwyz.iov.cloud.edd.vmd.service.application.event.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * MDM 选项值事件
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@NoArgsConstructor
public class MdmOptionCodeEvent extends MdmEvent {

    /**
     * 选项族代码
     */
    private String optionFamilyCode;

    /**
     * 选项值名称
     */
    private String name;

    /**
     * 本地名称
     */
    private String nameLocal;

    public MdmOptionCodeEvent(String eventType, String entityId, Long version, String code,
                               String optionFamilyCode, String name, String nameLocal, LocalDateTime occurredAt) {
        super(eventType, entityId, version, code, occurredAt);
        this.optionFamilyCode = optionFamilyCode;
        this.name = name;
        this.nameLocal = nameLocal;
    }

}
