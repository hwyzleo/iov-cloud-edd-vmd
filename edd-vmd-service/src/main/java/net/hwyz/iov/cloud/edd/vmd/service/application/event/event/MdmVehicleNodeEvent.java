package net.hwyz.iov.cloud.edd.vmd.service.application.event.event;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * MDM 车载节点事件
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@NoArgsConstructor
public class MdmVehicleNodeEvent extends MdmEvent {

    /**
     * 车载节点代码（兼容MDM侧nodeCode字段名）
     */
    @JsonAlias("nodeCode")
    private String code;

    /**
     * 车载节点名称
     */
    private String name;

    /**
     * 车载节点英文名称（兼容MDM侧nameLocal字段名）
     */
    @JsonAlias("nameLocal")
    private String nameEn;

    /**
     * 设备分类（兼容MDM侧deviceCategory字段名）
     */
    @JsonAlias("deviceCategory")
    private String deviceCategory;

    /**
     * 功能域（兼容MDM侧functionalDomain字段名）
     */
    @JsonAlias("functionalDomain")
    private String funcDomain;

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * OTA支持类型（兼容MDM侧otaSupportType字段名）
     */
    @JsonAlias("otaSupportType")
    private String otaSupport;

    /**
     * 是否核心设备（兼容MDM侧isCoreNode字段名）
     */
    @JsonAlias("isCoreNode")
    private Boolean core;

    /**
     * 排序
     */
    private Integer sort;

    public MdmVehicleNodeEvent(String eventType, String entityId, Long version, String code,
                                String name, String nameEn, String deviceCategory,
                                String funcDomain, String nodeType, String otaSupport,
                                Boolean core, Integer sort, LocalDateTime occurredAt) {
        super(eventType, entityId, version, code, occurredAt);
        this.code = code;
        this.name = name;
        this.nameEn = nameEn;
        this.deviceCategory = deviceCategory;
        this.funcDomain = funcDomain;
        this.nodeType = nodeType;
        this.otaSupport = otaSupport;
        this.core = core;
        this.sort = sort;
    }

}
