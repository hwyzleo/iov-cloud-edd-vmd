package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VmdOutbox;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehImportEventReplayRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VmdOutboxRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdKafkaTopicReadiness;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdKafkaTopicRoutes;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Outbox Relay 单元测试（VMD-DSN-CR-051）
 * <p>
 * 验证按路由注册表 fail-fast 发布：未知逻辑事件类型置 DEAD、存量旧名 Topic 丢弃不改写、
 * 生产 Topic readiness=DOWN 时暂停发布。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxRelay 测试")
class OutboxRelayTest {

    private static final String EVENT_ID = "event-001";
    private static final String VIN = "HWYZTEST000000001";

    @Mock
    private VmdOutboxRepository vmdOutboxRepository;

    @Mock
    private VehImportEventReplayRepository vehImportEventReplayRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private VmdKafkaTopicRoutes topicRoutes;
    private VmdKafkaTopicReadiness readiness;
    private OutboxRelay relay;

    @BeforeEach
    void setUp() {
        topicRoutes = new VmdKafkaTopicRoutes(new net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.VmdKafkaTopicProperties());
        readiness = new VmdKafkaTopicReadiness();
        relay = new OutboxRelay(vmdOutboxRepository, vehImportEventReplayRepository, kafkaTemplate, topicRoutes, readiness);
        ReflectionTestUtils.setField(relay, "batchSize", 100);
        ReflectionTestUtils.setField(relay, "maxRetry", 3);
        ReflectionTestUtils.setField(relay, "retryIntervalSeconds", 60);
        ReflectionTestUtils.setField(relay, "gateProducer", true);
    }

    private VmdOutbox outbox(String eventType, String topic) {
        return VmdOutbox.builder()
                .eventId(EVENT_ID)
                .eventType(eventType)
                .topic(topic)
                .messageKey(VIN)
                .payload("{\"vin\":\"" + VIN + "\"}")
                .publishState("PENDING")
                .retryCount(0)
                .build();
    }

    private void stubSendSuccess(String topic) {
        SendResult<String, String> result = mock(SendResult.class);
        RecordMetadata metadata = new RecordMetadata(new TopicPartition(topic, 0), 0L, 0, 0, 0L, 1, 1);
        when(result.getRecordMetadata()).thenReturn(metadata);
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(result));
    }

    @Test
    @DisplayName("已登记事件按标准 Topic 发布（Key=VIN）")
    void publishRegisteredEventToStandardTopic() {
        VmdOutbox outbox = outbox("VehicleProduceEvent", "vmd.vehicle-produce");
        when(vmdOutboxRepository.selectPendingMessages(100)).thenReturn(List.of(outbox));
        stubSendSuccess("vmd.vehicle-produce");

        relay.relayMessages();

        verify(kafkaTemplate).send("vmd.vehicle-produce", VIN, outbox.getPayload());
        ArgumentCaptor<VmdOutbox> captor = ArgumentCaptor.forClass(VmdOutbox.class);
        verify(vmdOutboxRepository).update(captor.capture());
        assertEquals("PUBLISHED", captor.getValue().getPublishState());
    }

    @Test
    @DisplayName("未知逻辑事件类型 fail-fast：置 DEAD，不发送")
    void unknownEventTypeDeadNoSend() {
        VmdOutbox outbox = outbox("UnknownEvent", "whatever-topic");
        when(vmdOutboxRepository.selectPendingMessages(100)).thenReturn(List.of(outbox));

        relay.relayMessages();

        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        ArgumentCaptor<VmdOutbox> captor = ArgumentCaptor.forClass(VmdOutbox.class);
        verify(vmdOutboxRepository).update(captor.capture());
        assertEquals("DEAD", captor.getValue().getPublishState());
        assertEquals(true, captor.getValue().getLastError().contains("未知逻辑事件类型"));
    }

    @Test
    @DisplayName("存量旧名 Topic 记录丢弃（不改写、不发旧 Topic）")
    void staleOldTopicDeadNoSend() {
        VmdOutbox outbox = outbox("VehicleProduceEvent", "vmd.vehicle.produce.event");
        when(vmdOutboxRepository.selectPendingMessages(100)).thenReturn(List.of(outbox));

        relay.relayMessages();

        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        ArgumentCaptor<VmdOutbox> captor = ArgumentCaptor.forClass(VmdOutbox.class);
        verify(vmdOutboxRepository).update(captor.capture());
        assertEquals("DEAD", captor.getValue().getPublishState());
        assertEquals(true, captor.getValue().getLastError().contains("标准路由不一致"));
    }

    @Test
    @DisplayName("生产 Topic readiness=DOWN 且门禁开启：跳过整批发布")
    void readinessDownGatesBatch() {
        readiness.markProducerTopicsDown(
                List.of(new VmdKafkaTopicReadiness.TopicConfigMismatch(
                        "vmd.vehicle-produce", "PARTITIONS", "3", "1")),
                "生产 Topic 配置漂移");

        relay.relayMessages();

        // 门禁在扫描 Outbox 之前生效，不触碰仓储与 Kafka
        verify(vmdOutboxRepository, never()).selectPendingMessages(100);
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        verify(vmdOutboxRepository, never()).update(any());
    }

    @Test
    @DisplayName("readiness=DOWN 但门禁关闭：仍发布")
    void readinessDownGateDisabledStillPublishes() {
        ReflectionTestUtils.setField(relay, "gateProducer", false);
        readiness.markProducerTopicsDown(
                List.of(new VmdKafkaTopicReadiness.TopicConfigMismatch(
                        "vmd.vehicle-produce", "PARTITIONS", "3", "1")),
                "生产 Topic 配置漂移");
        VmdOutbox outbox = outbox("VehicleProduceEvent", "vmd.vehicle-produce");
        when(vmdOutboxRepository.selectPendingMessages(100)).thenReturn(List.of(outbox));
        stubSendSuccess("vmd.vehicle-produce");

        relay.relayMessages();

        verify(kafkaTemplate).send("vmd.vehicle-produce", VIN, outbox.getPayload());
    }
}
