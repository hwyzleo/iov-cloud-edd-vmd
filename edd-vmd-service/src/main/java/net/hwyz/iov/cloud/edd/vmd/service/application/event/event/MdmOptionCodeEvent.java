package net.hwyz.iov.cloud.edd.vmd.service.application.event.event;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * MDM 选项值事件
 *
 * @author hwyz_leo
 */
@Getter
@Setter
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
     * 英文名称
     */
    private String nameEn;

    /**
     * 选项值
     */
    private String val;

    /**
     * 是否启用
     */
    private Boolean enable;

    /**
     * 排序
     */
    private Integer sort;

    public MdmOptionCodeEvent(String eventType, String entityId, Long version, String code,
                               String optionFamilyCode, String name, String nameEn, String val,
                               Boolean enable, Integer sort, LocalDateTime occurredAt) {
        super(eventType, entityId, version, code, occurredAt);
        this.optionFamilyCode = optionFamilyCode;
        this.name = name;
        this.nameEn = nameEn;
        this.val = val;
        this.enable = enable;
        this.sort = sort;
    }

}
