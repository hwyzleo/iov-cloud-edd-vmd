package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import net.hwyz.iov.cloud.framework.common.domain.DomainObj;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * 车系领域对象
 *
 * @author hwyz_leo
 */
@Slf4j
@Getter
@Setter
@SuperBuilder
public class CarLine implements DomainObj<CarLine> {

    /**
     * 主键
     */
    private Long id;

    /**
     * 品牌代码
     */
    private String brandCode;

    /**
     * 车系代码
     */
    private String code;

    /**
     * 车系名称
     */
    private String name;

    /**
     * 车系英文名称
     */
    private String nameEn;

    /**
     * 是否启用
     */
    private Boolean enable;

    /**
     * 排序
     */
    private Integer sort;

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
