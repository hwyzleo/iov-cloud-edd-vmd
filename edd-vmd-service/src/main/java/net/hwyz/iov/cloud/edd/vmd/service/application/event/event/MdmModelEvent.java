package net.hwyz.iov.cloud.edd.vmd.service.application.event.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * MDM 车型事件
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@NoArgsConstructor
public class MdmModelEvent extends MdmEvent {

    /**
     * 车型名称
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

    public MdmModelEvent(String eventType, String entityId, Long version, String code,
                         String name, String platformCode, String carLineCode, LocalDateTime occurredAt) {
        super(eventType, entityId, version, code, occurredAt);
        this.name = name;
        this.platformCode = platformCode;
        this.carLineCode = carLineCode;
    }

}
