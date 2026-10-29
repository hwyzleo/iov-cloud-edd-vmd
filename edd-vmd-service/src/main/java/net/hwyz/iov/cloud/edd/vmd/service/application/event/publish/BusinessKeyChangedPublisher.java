package net.hwyz.iov.cloud.edd.vmd.service.application.event.publish;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.BusinessKeyChangedEvent;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.DeviceBusinessKey;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VmdOutbox;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VmdOutboxRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdKafkaLogicalTopic;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdKafkaTopicRoutes;
import com.fasterxml.jackson.core.JsonProcessingException;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.VmdEventJsonCodec;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 设备业务密钥变更事件发布类（CR-055 §7.4）
 * <p>
 * 经 vmd_outbox → OutboxRelay 发布到 Kafka（Key=keyId）。
 * 事件 payload 不含 kmsKeyRef、Wrapped Key 或密钥材料。
 * <p>
 * payload 采用受控专用 ISO codec（VMD-DSN-CR-070 既有边界例外，见
 * {@code VmdEventJsonCodec}）序列化：时间字段 occurredAt 固定输出 ISO-8601（UTC）字符串
 * （如 {@code 2026-10-27T14:57:34.340Z}），与消费端 TSP（TSP-DSN-CR-013 双读）契约一致，
 * 避免 hutool 默认将 {@code Instant} 序列化为 epoch 毫秒数字导致的单位歧义；
 * 不随全局 date-format=epoch-millis 模式退回数字（CR-070 §5.10.2 例外登记）。
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@Slf4j
@Component
public class BusinessKeyChangedPublisher {

    private static final String EVENT_TYPE = "BusinessKeyChangedEvent";
    private static final String AGGREGATE_TYPE = "DEVICE_BUSINESS_KEY";
    private static final String SOURCE_TYPE = "BUSINESS_KEY";

    private final VmdOutboxRepository vmdOutboxRepository;
    private final VmdKafkaTopicRoutes topicRoutes;
    private final VmdEventJsonCodec eventJsonCodec;

    public BusinessKeyChangedPublisher(VmdOutboxRepository vmdOutboxRepository,
                                       VmdKafkaTopicRoutes topicRoutes,
                                       VmdEventJsonCodec eventJsonCodec) {
        this.vmdOutboxRepository = vmdOutboxRepository;
        this.topicRoutes = topicRoutes;
        this.eventJsonCodec = eventJsonCodec;
    }

    /**
     * 发布业务密钥变更事件
     *
     * @param key   业务密钥
     * @param state 变更动作（ACTIVE/ROTATED/REVOKING/REVOKED/EXPIRED）
     */
    public void publish(DeviceBusinessKey key, String state) {
        if (key == null || key.getKeyId() == null) {
            log.warn("业务密钥变更事件跳过：keyId 为空");
            return;
        }

        BusinessKeyChangedEvent event = new BusinessKeyChangedEvent(
                key.getDeviceSn(),
                key.getBusinessDomain(),
                key.getPurpose(),
                key.getKeyId(),
                key.getBusinessKeyVersion(),
                state,
                Instant.now()
        );

        String payload;
        try {
            payload = eventJsonCodec.writeAsString(event);
        } catch (JsonProcessingException e) {
            log.error("业务密钥变更事件序列化失败，跳过发布: keyId={}, state={}", key.getKeyId(), state, e);
            return;
        }

        VmdOutbox outbox = VmdOutbox.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(EVENT_TYPE)
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(key.getKeyId())
                .aggregateVersion(System.currentTimeMillis())
                .topic(topicRoutes.topicName(VmdKafkaLogicalTopic.BUSINESS_KEY_CHANGED))
                .messageKey(key.getKeyId())
                .payload(payload)
                .publishState("PENDING")
                .retryCount(0)
                .sourceType(SOURCE_TYPE)
                .sourceRefId(key.getRequestId())
                .createTime(LocalDateTime.now())
                .build();
        vmdOutboxRepository.insert(outbox);

        log.info("设备业务密钥变更事件写入Outbox: deviceSn={}, domain={}, purpose={}, keyId={}, state={}",
                key.getDeviceSn(), key.getBusinessDomain(), key.getPurpose(), key.getKeyId(), state);
    }
}
