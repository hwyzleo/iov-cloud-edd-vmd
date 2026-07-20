package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * <p>
 * 软件实装时态表 持久化对象
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-07-18
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_part_software_installation")
public class PartSoftwareInstallationPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 实例锚（关联part_info.id）
     */
    @TableField("part_id")
    private Long partId;

    /**
     * 绑定ID（关联vehicle_part.id，可空；游离件刷写允许无VIN）
     */
    @TableField("binding_id")
    private Long bindingId;

    /**
     * 观测时VIN快照（换件后历史追溯用，非绑定权威）
     */
    @TableField("vin_snapshot")
    private String vinSnapshot;

    /**
     * 逻辑升级目标（如TBOX_BOOT/TBOX_APP/CALIBRATION/Software Cluster）
     */
    @TableField("software_target_code")
    private String softwareTargetCode;

    /**
     * 软件零件号（引用MDM软件零件主数据）
     */
    @TableField("software_part_no")
    private String softwarePartNo;

    /**
     * 实际安装版本
     */
    @TableField("software_version")
    private String softwareVersion;

    /**
     * 实装制品摘要（可空，OTA对账/防错刷）
     */
    @TableField("artifact_hash")
    private String artifactHash;

    /**
     * A/B分区/逻辑槽位（可空）
     */
    @TableField("slot")
    private String slot;

    /**
     * 当前清单取ACTIVE
     */
    @TableField("install_state")
    private String installState;

    /**
     * 变更类型（INITIAL/UPGRADE/ROLLBACK/REFLASH/REPAIR）
     */
    @TableField("change_type")
    private String changeType;

    /**
     * 版本有效开始时间
     */
    @TableField("effective_from")
    private Instant effectiveFrom;

    /**
     * 版本有效结束时间（当前记录为NULL）
     */
    @TableField("effective_to")
    private Instant effectiveTo;

    /**
     * 来源（EOL/VEHICLE_REPORT/OTA/AFTER_SALES/MANUAL）
     */
    @TableField("source")
    private String source;

    /**
     * 来源事件幂等键
     */
    @TableField("source_event_id")
    private String sourceEventId;

    /**
     * 源端观测时间
     */
    @TableField("reported_at")
    private Instant reportedAt;

    /**
     * 物理实例软件清单单调版本（乱序保护）
     */
    @TableField("inventory_version")
    private Long inventoryVersion;

    /**
     * 软件类型（CR-043）
     */
    @TableField("software_type")
    private String softwareType;

    /**
     * 刷写结果（CR-043）
     */
    @TableField("flash_result")
    private String flashResult;

    /**
     * 是否已确认（CR-045）
     */
    @TableField("is_confirmed")
    private Boolean isConfirmed;

    /**
     * 来源事件时间（CR-045）
     */
    @TableField("source_event_time")
    private Instant sourceEventTime;
}
