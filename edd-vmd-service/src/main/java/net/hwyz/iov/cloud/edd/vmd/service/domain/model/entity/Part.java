package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.framework.common.domain.DomainObj;
import java.time.LocalDateTime;

/**
 * 零件领域对象
 *
 * @author hwyz_leo
 */
@Slf4j
@Getter
@Setter
@SuperBuilder
public class Part implements DomainObj<Part> {

    /**
     * 主键
     */
    private Long id;

    /**
     * 零件号（对齐MDM code）
     */
    private String code;

    /**
     * 零件中文名称
     */
    private String name;

    /**
     * 本地化名称
     */
    private String nameLocal;

    /**
     * 零件类型
     */
    private String partType;

    /**
     * 是否是软件
     */
    @Builder.Default
    private Boolean isSoftware = false;

    /**
     * 零件状态
     */
    private String status;

    /**
     * 是否精准追溯
     */
    private Boolean isAccuratelyTraced;

    /**
     * 车辆节点代码
     */
    private String vehicleNodeCode;

    /**
     * 供应商代码
     */
    private String supplierCode;

    /**
     * 是否支持FOTA升级
     */
    private Boolean fotaUpgradeable;

    /**
     * 数据来源：MDM=来自MDM系统，MANUAL=本地手动维护
     */
    private SourceType source;

    /**
     * MDM侧实体主键ID
     */
    private String externalRefId;

    /**
     * MDM侧实体版本号
     */
    private Long externalVersion;

    /**
     * 最后一次同步时间
     */
    private LocalDateTime lastSyncTime;

}
