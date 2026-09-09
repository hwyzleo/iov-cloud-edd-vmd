package net.hwyz.iov.cloud.edd.vmd.service.application.event.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * MDM 配置事件
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@NoArgsConstructor
public class MdmConfigurationEvent extends MdmEvent {

    /**
     * 配置名称
     */
    private String name;

    /**
     * 本地化名称（CR-047：对齐 MDM nameLocal 契约）
     */
    private String nameLocal;

    /**
     * 版本代码（CR-047：Configuration 唯一直接产品树父引用）
     */
    private String variantCode;

    /**
     * 备注
     */
    private String description;

    public MdmConfigurationEvent(String eventType, String entityId, Long version, String code,
                                  String name, String nameLocal, String variantCode, String description,
                                  LocalDateTime occurredAt) {
        super(eventType, entityId, version, code, occurredAt);
        this.name = name;
        this.nameLocal = nameLocal;
        this.variantCode = variantCode;
        this.description = description;
    }

}
