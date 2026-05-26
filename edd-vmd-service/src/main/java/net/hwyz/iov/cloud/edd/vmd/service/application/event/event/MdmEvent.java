package net.hwyz.iov.cloud.edd.vmd.service.application.event.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * MDM 事件基类
 * 用于接收 MDM 系统推送的品牌/车系/平台变更事件
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@AllArgsConstructor
public abstract class MdmEvent {

    /**
     * 事件类型：CREATED / UPDATED / DELETED
     */
    private String eventType;

    /**
     * MDM侧实体主键ID
     */
    private String entityId;

    /**
     * MDM侧实体版本号
     */
    private Long version;

    /**
     * 实体代码
     */
    private String code;

    /**
     * 事件发生时间
     */
    private LocalDateTime occurredAt;

}
