package net.hwyz.iov.cloud.edd.vmd.service.application.event.publish;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.TboxV2cKeyProvisionedEvent;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BusinessKeyProvisioningItem;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VmdOutbox;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VmdOutboxRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdKafkaLogicalTopic;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdKafkaTopicRoutes;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * TBOX V2C 业务密钥件内导入完成事件发布类（CR-058 §6.2）。
 * <p>
 * 在完成事务内写入 vmd_outbox；eventId 生成后进入 payload，Relay 不注入 header。
 * 同一 requestId 最多产生一个逻辑成功事件；payload 不含 VIN、HSM UID、kmsKeyRef、
 * Wrapped Key、证书或密钥材料。
 * <p>
 * payload 采用 Jackson 序列化，时间字段 occurredAt 输出 ISO-8601（UTC）字符串
 * （如 {@code 2026-10-27T14:57:34.340Z}），与消费端 Jackson 反序列化契约一致，
 * 避免 hutool 默认将 {@code Instant} 序列化为 epoch 毫秒数字导致的单位歧义。
 *
 * @author hwyz_leo
 * @since 2026-10-12
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TboxV2cKeyProvisionedPublisher {

    private static final String EVENT_TYPE = "TboxV2cKeyProvisionedEvent";
    private static final String AGGREGATE_TYPE = "BUSINESS_KEY_PROVISIONING";
    private static final String SOURCE_TYPE = "BUSINESS_KEY_PROVISIONING";
    private static final String RESULT_SUCCEEDED = "SUCCEEDED";

    private final VmdOutboxRepository vmdOutboxRepository;
    private final VmdKafkaTopicRoutes topicRoutes;
    private final ObjectMapper objectMapper;

    /**
     * 写入专用完成事件（须在完成事务内调用）
     *
     * @param requestId        TBOX 稳定 requestId
     * @param deviceSn         TBOX 序列号（Kafka Key）
     * @param businessDomain   业务域
     * @param succeededItems   完整成功的用途明细（purpose/keyId/version）
     */
    public void publish(String requestId, String deviceSn, String businessDomain,
                        List<BusinessKeyProvisioningItem> succeededItems) {
        if (requestId == null || deviceSn == null || succeededItems == null || succeededItems.isEmpty()) {
            log.warn("TboxV2cKeyProvisioned 事件跳过：参数不完整 requestId={}", requestId);
            return;
        }

        String eventId = UUID.randomUUID().toString();
        List<TboxV2cKeyProvisionedEvent.Key> keys = succeededItems.stream()
                .map(item -> new TboxV2cKeyProvisionedEvent.Key(
                        item.getPurpose(), item.getKeyId(), item.getBusinessKeyVersion()))
                .toList();

        TboxV2cKeyProvisionedEvent event = new TboxV2cKeyProvisionedEvent(
                eventId, requestId, deviceSn, businessDomain, keys, RESULT_SUCCEEDED, Instant.now());

        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.error("TboxV2cKeyProvisioned 事件序列化失败，跳过发布: requestId={}, deviceSn={}", requestId, deviceSn, e);
            return;
        }

        VmdOutbox outbox = VmdOutbox.builder()
                .eventId(eventId)
                .eventType(EVENT_TYPE)
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(requestId)
                .aggregateVersion(System.currentTimeMillis())
                .topic(topicRoutes.topicName(VmdKafkaLogicalTopic.TBOX_V2C_KEY_PROVISIONED))
                .messageKey(deviceSn)
                .payload(payload)
                .publishState("PENDING")
                .retryCount(0)
                .sourceType(SOURCE_TYPE)
                .sourceRefId(requestId)
                .createTime(LocalDateTime.now())
                .build();
        vmdOutboxRepository.insert(outbox);

        log.info("TboxV2cKeyProvisioned 事件写入Outbox: requestId={}, deviceSn={}, domain={}, purposes={}",
                requestId, deviceSn, businessDomain, keys.size());
    }
}
