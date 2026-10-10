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
 * 零件导入后置处理重放动作明细表 持久化对象
 * <p>
 * VMD-DSN-CR-056: 零件导入后置处理人工重放（US-062）
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_part_import_postprocess_replay_item")
public class PartImportPostProcessReplayItemPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联重放任务ID
     */
    @TableField("replay_id")
    private String replayId;

    /**
     * 零件编码
     */
    @TableField("part_code")
    private String partCode;

    /**
     * 零件序列号
     */
    @TableField("sn")
    private String sn;

    /**
     * 动作类型
     */
    @TableField("action_type")
    private String actionType;

    /**
     * 动作幂等键
     */
    @TableField("idempotency_key")
    private String idempotencyKey;

    /**
     * 动作状态：PENDING/RUNNING/QUEUED/SUCCESS/SKIPPED/FAILED_RETRYABLE/FAILED_FINAL
     */
    @TableField("status")
    private String status;

    /**
     * 尝试次数
     */
    @TableField("attempt_count")
    private Integer attemptCount;

    /**
     * 事件动作生成的事件ID
     */
    @TableField("event_id")
    private String eventId;

    /**
     * 事件动作关联的Outbox记录ID
     */
    @TableField("outbox_id")
    private Long outboxId;

    /**
     * 错误码
     */
    @TableField("error_code")
    private String errorCode;

    /**
     * 错误信息（截断）
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 跳过原因（SKIPPED时记录）
     */
    @TableField("skip_reason")
    private String skipReason;

    /**
     * 开始时间
     */
    @TableField("started_at")
    private LocalDateTime startedAt;

    /**
     * 结束时间
     */
    @TableField("finished_at")
    private LocalDateTime finishedAt;
}
