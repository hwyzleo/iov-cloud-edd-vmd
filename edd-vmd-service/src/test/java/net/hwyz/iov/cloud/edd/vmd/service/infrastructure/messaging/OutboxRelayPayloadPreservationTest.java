package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VmdOutbox;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehImportEventReplayRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VmdOutboxRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdKafkaTopicReadiness;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdKafkaTopicRoutes;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.support.GenericMessage;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Outbox Relay 历史 payload 原样补发测试（VMD-DSN-CR-070 §3/§5）
 * <p>
 * 锁定：Relay 直接以存储的 payload 字符串发布（KafkaTemplate<String,String>），
 * 对历史 Outbox / DLT 消息<b>不重编码、不重新映射时间字段</b>——
 * 兼容阶段与灰度阶段都不会因 JSON 开关改变已冻结的载荷字节。
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
@ExtendWith(MockitoExtension.class)
class OutboxRelayPayloadPreservationTest {

    private static final String TOPIC = "vmd.tbox-v2c-key-provisioned";
    private static final String KEY = "00000002AA00000001";

    /**
     * 模拟一条历史（d3751fe2 之前或任意冻结格式）Outbox 载荷：时间字段为 epoch 毫秒数字。
     * Relay 必须原样发送，不得因全局/专用 mapper 重新序列化。
     */
    private static final String LEGACY_PAYLOAD =
            "{\"eventId\":\"e1\",\"requestId\":\"rid-1\",\"deviceSn\":\"" + KEY
                    + "\",\"businessDomain\":\"V2C_COMM\",\"keys\":[{\"purpose\":\"UP_LINK_AEAD\","
                    + "\"keyId\":\"vmd-key-1\",\"businessKeyVersion\":3}],\"result\":\"SUCCEEDED\","
                    + "\"occurredAt\":1793191412340}";

    @Mock
    private VmdOutboxRepository vmdOutboxRepository;

    @Mock
    private VehImportEventReplayRepository vehImportEventReplayRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private VmdKafkaTopicRoutes topicRoutes;

    @Mock
    private VmdKafkaTopicReadiness topicReadiness;

    private OutboxRelay relay;

    @BeforeEach
    void setUp() {
        when(topicReadiness.producerTopicsState())
                .thenReturn(VmdKafkaTopicReadiness.State.UP);
        relay = new OutboxRelay(vmdOutboxRepository, vehImportEventReplayRepository,
                kafkaTemplate, topicRoutes, topicReadiness);
        // 手工构造绕过 @Value 注入：显式注入门禁与批次参数
        org.springframework.test.util.ReflectionTestUtils.setField(relay, "gateProducer", true);
        org.springframework.test.util.ReflectionTestUtils.setField(relay, "batchSize", 100);
    }

    @Test
    void relay_publishesLegacyPayloadAsIs_noReEncode() {
        // Given：一条历史 PENDING 消息（epoch 毫秒数字 occurredAt）
        VmdOutbox outbox = VmdOutbox.builder()
                .eventId("e1")
                .eventType("TboxV2cKeyProvisionedEvent")
                .aggregateId("rid-1")
                .topic(TOPIC)
                .messageKey(KEY)
                .payload(LEGACY_PAYLOAD)
                .publishState("PENDING")
                .retryCount(0)
                .sourceType("BUSINESS_KEY_PROVISIONING")
                .build();
        when(vmdOutboxRepository.selectPendingMessages(anyInt())).thenReturn(List.of(outbox));
        when(topicRoutes.resolve("TboxV2cKeyProvisionedEvent")).thenReturn(TOPIC);

        // 返回未完成 future，relay 调用后手动 complete + join，确定性驱动 whenComplete 回调
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(future);

        // When
        relay.relayMessages();

        // 完成发送结果（recordMetadata 需非 null，handlePublishSuccess 会读取 partition/offset）
        org.apache.kafka.clients.producer.RecordMetadata meta = new org.apache.kafka.clients.producer.RecordMetadata(
                new org.apache.kafka.common.TopicPartition(TOPIC, 0), 0L, 0L, 0L, 0L, 0, 0);
        SendResult<String, String> result = new SendResult<>(
                new ProducerRecord<>(TOPIC, KEY, LEGACY_PAYLOAD), meta);
        future.complete(result);
        future.join();

        // Then：发送的 value 必须等于存储 payload 原串（字节级原样，不重编码）
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(TOPIC), eq(KEY), valueCaptor.capture());
        assertEquals(LEGACY_PAYLOAD, valueCaptor.getValue(), "历史 Outbox payload 必须原样补发");

        // 且状态推进为 PUBLISHED
        verify(vmdOutboxRepository).update(argThat(o -> "PUBLISHED".equals(o.getPublishState())));
    }

    @Test
    void relay_doesNotPublishWhenProducerReadinessDown() {
        when(topicReadiness.producerTopicsState())
                .thenReturn(VmdKafkaTopicReadiness.State.DOWN);
        when(topicReadiness.producerReason()).thenReturn("test-down");

        relay.relayMessages();

        verify(vmdOutboxRepository, never()).selectPendingMessages(anyInt());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }
}
