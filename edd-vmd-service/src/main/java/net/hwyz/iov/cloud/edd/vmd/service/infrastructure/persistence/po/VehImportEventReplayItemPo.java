package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 车辆导入事件补发逐项动作审计表 持久化对象
 * <p>
 * VMD-DSN-CR-057: 车辆导入补发逐项动作审计
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_veh_import_event_replay_item")
public class VehImportEventReplayItemPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联补发任务ID
     */
    @TableField("replay_id")
    private String replayId;

    /**
     * 动作类型
     */
    @TableField("action_type")
    private String actionType;

    /**
     * 聚合类型：VEHICLE/VEHICLE_LIFECYCLE/VEHICLE_PART/PART_SOFTWARE_INSTALLATION
     */
    @TableField("aggregate_type")
    private String aggregateType;

    /**
     * 聚合ID（VIN/绑定ID/零件ID）
     */
    @TableField("aggregate_id")
    private String aggregateId;

    /**
     * 聚合版本（事件序/软件清单版本/0）
     */
    @TableField("aggregate_version")
    private Long aggregateVersion;

    /**
     * 原批次候选记录标识（如 partCode:sn，可空）
     */
    @TableField("source_record_id")
    private String sourceRecordId;

    /**
     * 事件动作生成的事件ID
     */
    @TableField("event_id")
    private String eventId;

    /**
     * 动作状态：PENDING/RUNNING/QUEUED/SUCCESS/SKIPPED/FAILED_RETRYABLE/FAILED_FINAL
     */
    @TableField("status")
    private String status;

    /**
     * 跳过原因（SKIPPED时记录）
     */
    @TableField("skip_reason")
    private String skipReason;

    /**
     * 失败原因（截断）
     */
    @TableField("failure_reason")
    private String failureReason;

    /**
     * 开始时间
     */
    @TableField("started_at")
    private LocalDateTime startedAt;

    /**
     * 完成时间
     */
    @TableField("completed_at")
    private LocalDateTime completedAt;
}
