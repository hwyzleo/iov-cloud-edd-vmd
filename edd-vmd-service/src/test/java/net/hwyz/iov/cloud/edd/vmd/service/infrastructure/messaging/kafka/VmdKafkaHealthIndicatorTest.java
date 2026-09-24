package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicProvisioningStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * VMD Kafka 健康检查单元测试（VMD-DSN-CR-051）
 * <p>
 * 验证 producerTopics 与 inventoryObservedConsumer 分离：
 * 框架 Provisioning 不可用 / 漂移 → 生产 DOWN；上游观测 Topic 未就绪不阻断整体健康。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VmdKafkaHealthIndicator 测试")
class VmdKafkaHealthIndicatorTest {

    @Mock
    private ObjectProvider<KafkaTopicProvisioningStatus> provisioningStatusProvider;

    private VmdKafkaTopicReadiness readiness;
    private VmdKafkaHealthIndicator indicator;

    @BeforeEach
    void setUp() {
        readiness = new VmdKafkaTopicReadiness();
        indicator = new VmdKafkaHealthIndicator(provisioningStatusProvider, readiness);
        ReflectionTestUtils.setField(indicator, "observedEnabled", false);
    }

    private KafkaTopicProvisioningStatus provisioning(KafkaTopicProvisioningStatus.State state) {
        return new KafkaTopicProvisioningStatus() {
            @Override
            public State state() {
                return state;
            }

            @Override
            public Set<String> missingTopics() {
                return state == State.NOT_READY ? Set.of("vmd.vehicle-produce") : Set.of();
            }

            @Override
            public java.util.Optional<Throwable> lastFailure() {
                return java.util.Optional.of(new IllegalStateException("broker down"));
            }

            @Override
            public java.util.Optional<java.time.Instant> nextRetryAt() {
                return java.util.Optional.empty();
            }
        };
    }

    @Test
    @DisplayName("框架 Provisioning NOT_READY：生产 DOWN，整体 DOWN")
    void provisioningNotReady_healthDown() {
        when(provisioningStatusProvider.getIfAvailable()).thenReturn(provisioning(KafkaTopicProvisioningStatus.State.NOT_READY));
        Health health = indicator.health();
        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("DOWN", health.getDetails().get("producerTopics"));
        assertEquals("NOT_READY", health.getDetails().get("producerTopics.state"));
    }

    @Test
    @DisplayName("框架 READY + VMD 无漂移：生产 UP，整体 UP")
    void allReady_healthUp() {
        when(provisioningStatusProvider.getIfAvailable()).thenReturn(provisioning(KafkaTopicProvisioningStatus.State.READY));
        readiness.markProducerTopicsUp();
        Health health = indicator.health();
        assertEquals(Status.UP, health.getStatus());
        assertEquals("UP", health.getDetails().get("producerTopics"));
    }

    @Test
    @DisplayName("VMD 漂移校验 DOWN：生产 DOWN，整体 DOWN")
    void driftDown_healthDown() {
        when(provisioningStatusProvider.getIfAvailable()).thenReturn(provisioning(KafkaTopicProvisioningStatus.State.READY));
        readiness.markProducerTopicsDown(
                java.util.List.of(new VmdKafkaTopicReadiness.TopicConfigMismatch(
                        "vmd.vehicle-produce", "PARTITIONS", "3", "1")),
                "生产 Topic 配置漂移");
        Health health = indicator.health();
        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("DOWN", health.getDetails().get("producerTopics"));
        assertEquals(true, ((String) health.getDetails().get("producerTopics.mismatches")).contains("PARTITIONS"));
    }

    @Test
    @DisplayName("消费 Topic 暂不存在：独立 NOT_PRESENT 信息项，不阻断整体健康")
    void consumerNotPresent_healthStillUp() {
        when(provisioningStatusProvider.getIfAvailable()).thenReturn(provisioning(KafkaTopicProvisioningStatus.State.READY));
        readiness.markProducerTopicsUp();
        readiness.markInventoryObservedNotPresent();
        ReflectionTestUtils.setField(indicator, "observedEnabled", true);
        Health health = indicator.health();
        assertEquals(Status.UP, health.getStatus());
        assertEquals("NOT_PRESENT", health.getDetails().get("inventoryObservedConsumer"));
    }

    @Test
    @DisplayName("消费 Topic 探测失败（ACL/Broker）：整体 DOWN")
    void consumerDown_healthDown() {
        when(provisioningStatusProvider.getIfAvailable()).thenReturn(provisioning(KafkaTopicProvisioningStatus.State.READY));
        readiness.markProducerTopicsUp();
        readiness.markInventoryObservedDown("ACL denied");
        ReflectionTestUtils.setField(indicator, "observedEnabled", true);
        Health health = indicator.health();
        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("DOWN", health.getDetails().get("inventoryObservedConsumer"));
    }

    @Test
    @DisplayName("Provisioning 未启用（无 Bean）：视为健康，标注 DISABLED")
    void provisioningDisabled_healthUp() {
        when(provisioningStatusProvider.getIfAvailable()).thenReturn(null);
        Health health = indicator.health();
        assertEquals(Status.UP, health.getStatus());
        assertEquals("UP", health.getDetails().get("producerTopics"));
        assertEquals("DISABLED", health.getDetails().get("producerTopics.state"));
        assertNull(health.getDetails().get("inventoryObservedConsumer"),
                "消费者未启用时不应上报消费健康项");
    }
}
