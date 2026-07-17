package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.framework.common.domain.BaseDo;

import java.time.LocalDateTime;

/**
 * 通用 Outbox 领域实体
 * <p>
 * VMD-DSN-CR-039: 车辆导入成功事件人工补发（Kafka Outbox 模式）
 * <p>
 * 支持可靠消息发布，由 Outbox Relay 扫描并发布到 Kafka
 *
 * @author hwyz_leo
 * @since 2026-07-17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmdOutbox extends BaseDo<Long> {

    private Long id;
    private String eventId;
    private String eventType;
    private String aggregateType;
    private String aggregateId;
    private Long aggregateVersion;
    private String topic;
    private String messageKey;
    private String payload;
    private String publishState;
    private Integer retryCount;
    private LocalDateTime nextRetryTime;
    private LocalDateTime publishedAt;
    private String lastError;
    private String sourceType;
    private String sourceRefId;
    private LocalDateTime createTime;
}
