package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import net.hwyz.iov.cloud.framework.common.domain.BaseDo;
import net.hwyz.iov.cloud.framework.common.domain.DomainObj;

import java.time.Instant;
import java.util.Date;

/**
 * 软件实装时态记录领域对象
 * <p>
 * 以物理实例/绑定为锚维护当前软件清单+历史版本时间线
 * VIN↔软件零件为派生关系
 * UK (partId, softwareTargetCode, slot) 单条ACTIVE
 * 事件幂等 UK(source, sourceEventId, softwareTargetCode, slot)
 *
 * @author hwyz_leo
 */
@Slf4j
@Getter
@Setter
@SuperBuilder
public class PartSoftwareInstallation extends BaseDo<Long> implements DomainObj<PartSoftwareInstallation> {

    /**
     * 主键
     */
    private Long id;

    /**
     * 实例锚（关联part_info.id）
     */
    private Long partId;

    /**
     * 绑定ID（关联vehicle_part.id，可空；游离件刷写允许无VIN）
     */
    private Long bindingId;

    /**
     * 观测时VIN快照（换件后历史追溯用，非绑定权威）
     */
    private String vinSnapshot;

    /**
     * 逻辑升级目标（如TBOX_BOOT/TBOX_APP/CALIBRATION/Software Cluster）
     */
    private String softwareTargetCode;

    /**
     * 软件零件号（引用MDM软件零件主数据）
     */
    private String softwarePartNo;

    /**
     * 实际安装版本
     */
    private String softwareVersion;

    /**
     * 实装制品摘要（可空，OTA对账/防错刷）
     */
    private String artifactHash;

     /**
      * A/B分区/逻辑槽位（可空）
      */
     private String slot;

    /**
     * 当前清单取ACTIVE
     */
    private String installState;

    /**
     * 变更类型（INITIAL/UPGRADE/ROLLBACK/REFLASH/REPAIR）
     */
    private String changeType;

    /**
     * 版本有效开始时间
     */
    private Instant effectiveFrom;

    /**
     * 版本有效结束时间（当前记录为NULL）
     */
    private Instant effectiveTo;

    /**
     * 来源（EOL/VEHICLE_REPORT/OTA/AFTER_SALES/MANUAL）
     */
    private String source;

    /**
     * 来源事件幂等键
     */
    private String sourceEventId;

    /**
     * 源端观测时间
     */
    private Instant reportedAt;

    /**
     * 物理实例软件清单单调版本（乱序保护）
     */
    private Long inventoryVersion;

    /**
     * 软件类型（CR-043）
     */
    private String softwareType;

    /**
     * 刷写结果（CR-043）
     */
    private String flashResult;

    /**
     * 创建时间
     */
    private Date createTime;

    public void init() {
        stateInit();
        if (createTime == null) {
            createTime = new Date();
        }
    }
}
