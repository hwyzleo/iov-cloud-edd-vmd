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
     * 英文名称
     */
    private String nameEn;

    /**
     * 类型
     */
    private String type;

    /**
     * 是否必选
     */
    private Boolean mandatory;

    /**
     * 是否启用
     */
    private Boolean enable;

    /**
     * 排序
     */
    private Integer sort;

    public MdmOptionFamilyEvent(String eventType, String entityId, Long version, String code,
                                 String name, String nameEn, String type, Boolean mandatory,
                                 Boolean enable, Integer sort, LocalDateTime occurredAt) {
        super(eventType, entityId, version, code, occurredAt);
        this.name = name;
        this.nameEn = nameEn;
        this.type = type;
        this.mandatory = mandatory;
        this.enable = enable;
        this.sort = sort;
    }

}
