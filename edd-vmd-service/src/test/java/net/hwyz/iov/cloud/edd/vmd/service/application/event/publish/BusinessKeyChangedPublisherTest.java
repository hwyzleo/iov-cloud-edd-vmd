package net.hwyz.iov.cloud.edd.vmd.service.application.event.publish;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.DeviceBusinessKey;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VmdOutbox;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VmdOutboxRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.VmdEventJsonCodec;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdKafkaLogicalTopic;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdKafkaTopicRoutes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * BusinessKeyChangedPublisher 单元测试
 * <p>
 * 锁定 wire 契约：occurredAt 必须以 ISO-8601（UTC）字符串序列化，
 * 与 TboxV2cKeyProvisionedPublisher 保持一致，避免 hutool 默认 epoch 毫秒数字的单位歧义。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
class BusinessKeyChangedPublisherTest {

    private static final String TOPIC = "vmd.business-key-changed";
    private static final String DEVICE_SN = "00000002AA00000001";
    private static final String DOMAIN = "V2C_COMM";
    private static final String PURPOSE = "UP_LINK_AEAD";
    private static final String KEY_ID = "vmd-key-100";
    private static final Long KEY_VERSION = 2L;
    private static final String REQUEST_ID = "kp-test-request-001";

    @Mock
    private VmdOutboxRepository vmdOutboxRepository;

    @Mock
    private VmdKafkaTopicRoutes topicRoutes;

    private ObjectMapper objectMapper;
    private BusinessKeyChangedPublisher publisher;

    @BeforeEach
    void setUp() {
        // 使用真实生产 codec（VmdEventJsonCodec，VMD-DSN-CR-070 既有边界例外），
        // 而非测试内临时构造的理想化 mapper——锁定受控专用 ISO codec 的真实行为。
        objectMapper = new VmdEventJsonCodec().mapper();
        publisher = new BusinessKeyChangedPublisher(vmdOutboxRepository, topicRoutes, new VmdEventJsonCodec());
        lenient().when(topicRoutes.topicName(VmdKafkaLogicalTopic.BUSINESS_KEY_CHANGED)).thenReturn(TOPIC);
    }

    @Test
    void publish_writesOutboxWithIso8601OccurredAt() throws Exception {
        // Given
        DeviceBusinessKey key = DeviceBusinessKey.builder()
                .deviceSn(DEVICE_SN)
                .businessDomain(DOMAIN)
                .purpose(PURPOSE)
                .keyId(KEY_ID)
                .businessKeyVersion(KEY_VERSION)
                .requestId(REQUEST_ID)
                .build();

        // When
        publisher.publish(key, "ACTIVE");

        // Then
        ArgumentCaptor<VmdOutbox> captor = ArgumentCaptor.forClass(VmdOutbox.class);
        verify(vmdOutboxRepository).insert(captor.capture());
        VmdOutbox outbox = captor.getValue();
        assertEquals(TOPIC, outbox.getTopic());
        assertEquals(KEY_ID, outbox.getMessageKey());
        assertEquals("BusinessKeyChangedEvent", outbox.getEventType());
        assertEquals(KEY_ID, outbox.getAggregateId());
        assertEquals(REQUEST_ID, outbox.getSourceRefId());

        // payload 契约：occurredAt 必须是 ISO-8601 字符串，且不是数字
        JsonNode node = objectMapper.readTree(outbox.getPayload());
        JsonNode occurredAt = node.get("occurredAt");
        assertNotNull(occurredAt, "occurredAt 必须存在");
        assertFalse(occurredAt.isNumber(), "occurredAt 不得为 epoch 数字");
        assertTrue(occurredAt.isTextual(), "occurredAt 必须是 ISO-8601 字符串");
        assertTrue(occurredAt.asText().matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z$"),
                "occurredAt 格式非法: " + occurredAt.asText());

        Instant parsed = Instant.parse(occurredAt.asText());
        Instant now = Instant.now();
        assertFalse(parsed.isAfter(now.plusSeconds(5)), "occurredAt 不应超前当前时间");
        assertFalse(parsed.isBefore(now.minusSeconds(5)), "occurredAt 不应落后当前时间");

        // 其余关键字段
        assertEquals(DEVICE_SN, node.get("deviceSn").asText());
        assertEquals(DOMAIN, node.get("businessDomain").asText());
        assertEquals(PURPOSE, node.get("purpose").asText());
        assertEquals(KEY_ID, node.get("keyId").asText());
        assertEquals(KEY_VERSION, node.get("businessKeyVersion").asLong());
        assertEquals("ACTIVE", node.get("state").asText());
    }

    @Test
    void publish_nullKey_skipsOutbox() {
        // When
        publisher.publish(null, "ACTIVE");

        // Then
        verify(vmdOutboxRepository, never()).insert(any());
    }

    @Test
    void publish_nullKeyId_skipsOutbox() {
        // Given
        DeviceBusinessKey key = DeviceBusinessKey.builder()
                .deviceSn(DEVICE_SN)
                .businessDomain(DOMAIN)
                .purpose(PURPOSE)
                .businessKeyVersion(KEY_VERSION)
                .requestId(REQUEST_ID)
                .build();

        // When
        publisher.publish(key, "ACTIVE");

        // Then
        verify(vmdOutboxRepository, never()).insert(any());
    }
}
