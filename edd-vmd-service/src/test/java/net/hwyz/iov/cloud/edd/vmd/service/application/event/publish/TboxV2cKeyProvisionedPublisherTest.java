package net.hwyz.iov.cloud.edd.vmd.service.application.event.publish;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.BusinessKeyProvisioningItem;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VmdOutbox;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VmdOutboxRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdKafkaLogicalTopic;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdKafkaTopicRoutes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * TboxV2cKeyProvisionedPublisher 单元测试
 * <p>
 * 锁定 wire 契约：occurredAt 必须以 ISO-8601（UTC）字符串序列化（如
 * {@code 2026-10-27T14:57:34.340Z}），而非 hutool 默认的 epoch 毫秒数字——
 * 后者会被 Jackson 按 epoch 秒解析，导致消费端 OCCURRED_AT_FUTURE 永久拒绝。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
class TboxV2cKeyProvisionedPublisherTest {

    private static final String TOPIC = "vmd.tbox-v2c-key-provisioned";
    private static final String REQUEST_ID = "kp-28faff584ab67dc72b234d309f9325de";
    private static final String DEVICE_SN = "00000002AA00000001";
    private static final String DOMAIN = "V2C_COMM";

    @Mock
    private VmdOutboxRepository vmdOutboxRepository;

    @Mock
    private VmdKafkaTopicRoutes topicRoutes;

    private ObjectMapper objectMapper;
    private TboxV2cKeyProvisionedPublisher publisher;

    @BeforeEach
    void setUp() {
        // 模拟 Spring Boot 默认 ObjectMapper：JavaTimeModule + 关闭 WRITE_DATES_AS_TIMESTAMPS
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        publisher = new TboxV2cKeyProvisionedPublisher(vmdOutboxRepository, topicRoutes, objectMapper);
        lenient().when(topicRoutes.topicName(VmdKafkaLogicalTopic.TBOX_V2C_KEY_PROVISIONED)).thenReturn(TOPIC);
    }

    @Test
    void publish_writesOutboxWithIso8601OccurredAt() throws Exception {
        // Given
        BusinessKeyProvisioningItem item = BusinessKeyProvisioningItem.builder()
                .purpose("UP_LINK_AEAD")
                .keyId("vmd-key-001")
                .businessKeyVersion(3L)
                .build();

        // When
        publisher.publish(REQUEST_ID, DEVICE_SN, DOMAIN, List.of(item));

        // Then
        ArgumentCaptor<VmdOutbox> captor = ArgumentCaptor.forClass(VmdOutbox.class);
        verify(vmdOutboxRepository).insert(captor.capture());
        VmdOutbox outbox = captor.getValue();
        assertEquals(TOPIC, outbox.getTopic());
        assertEquals(DEVICE_SN, outbox.getMessageKey());
        assertEquals("TboxV2cKeyProvisionedEvent", outbox.getEventType());
        assertEquals(REQUEST_ID, outbox.getAggregateId());

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
        assertEquals(REQUEST_ID, node.get("requestId").asText());
        assertEquals(DEVICE_SN, node.get("deviceSn").asText());
        assertEquals(DOMAIN, node.get("businessDomain").asText());
        assertEquals("SUCCEEDED", node.get("result").asText());
        assertEquals(1, node.get("keys").size());
        assertEquals("vmd-key-001", node.get("keys").get(0).get("keyId").asText());
        assertEquals(3L, node.get("keys").get(0).get("businessKeyVersion").asLong());
    }

    @Test
    void publish_emptySucceededItems_skipsOutbox() {
        // When
        publisher.publish(REQUEST_ID, DEVICE_SN, DOMAIN, List.of());

        // Then
        verify(vmdOutboxRepository, never()).insert(any());
    }

    @Test
    void publish_nullArgs_skipsOutbox() {
        // When
        publisher.publish(null, DEVICE_SN, DOMAIN, List.of(mockItem()));
        publisher.publish(REQUEST_ID, null, DOMAIN, List.of(mockItem()));

        // Then
        verify(vmdOutboxRepository, never()).insert(any());
    }

    private BusinessKeyProvisioningItem mockItem() {
        return BusinessKeyProvisioningItem.builder()
                .purpose("DOWN_LINK_AEAD")
                .keyId("vmd-key-002")
                .businessKeyVersion(1L)
                .build();
    }
}
