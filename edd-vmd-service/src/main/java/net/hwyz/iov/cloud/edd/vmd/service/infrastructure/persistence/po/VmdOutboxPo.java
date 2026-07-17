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
 * 通用 Outbox 表 持久化对象
 * <p>
 * VMD-DSN-CR-039: 车辆导入成功事件人工补发（Kafka Outbox 模式）
 *
 * @author hwyz_leo
 * @since 2026-07-17
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_vmd_outbox")
public class VmdOutboxPo extends BasePo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 事件唯一ID（幂等键）
     */
    @TableField("event_id")
    private String eventId;

    /**
     * 事件类型（如 VehicleProduceEvent）
     */
    @TableField("event_type")
    private String eventType;

    /**
     * 聚合类型（如 VEHICLE）
     */
    @TableField("aggregate_type")
    private String aggregateType;

    /**
     * 聚合ID（如 VIN）
     */
    @TableField("aggregate_id")
    private String aggregateId;

    /**
     * 聚合版本号
     */
    @TableField("aggregate_version")
    private Long aggregateVersion;

    /**
     * Kafka Topic
     */
    @TableField("topic")
    private String topic;

    /**
     * Kafka Message Key
     */
    @TableField("message_key")
    private String messageKey;

    /**
     * 事件payload（JSON）
     */
    @TableField("payload")
    private String payload;

    /**
     * 发布状态：PENDING/PUBLISHED/FAILED_RETRYABLE/DEAD
     */
    @TableField("publish_state")
    private String publishState;

    /**
     * 重试次数
     */
    @TableField("retry_count")
    private Integer retryCount;

    /**
     * 下次重试时间
     */
    @TableField("next_retry_time")
    private LocalDateTime nextRetryTime;

    /**
     * 发布时间
     */
    @TableField("published_at")
    private LocalDateTime publishedAt;

    /**
     * 最后一次错误信息
     */
    @TableField("last_error")
    private String lastError;

    /**
     * 来源类型（如 IMPORT_EVENT_REPLAY）
     */
    @TableField("source_type")
    private String sourceType;

    /**
     * 来源引用ID（如 replayId）
     */
    @TableField("source_ref_id")
    private String sourceRefId;
}
