package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.VmdActivationFactConsumerProperties;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.VmdKafkaTopicProperties;
import net.hwyz.iov.cloud.framework.kafka.support.KafkaAdminOperations;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.errors.TopicAuthorizationException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 首次激活事实 Topic 预检探针单元测试（VMD-DSN-CR-077 §8.2）
 * <p>
 * E2E 环境 Topic 可能尚未创建：探针必须<strong>不抛异常</strong>、记为 NOT_PRESENT、
 * 不阻塞启动，并发布结果事件供启动协调器自恢复；ACL 失败才记 DOWN。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VmdActivationFactTopicProbe 测试")
class VmdActivationFactTopicProbeTest {

    private static final String TOPIC = "tsp.user-vehicle-activation-fact";

    @Mock
    private Admin admin;
    @Mock
    private KafkaAdminOperations kafkaAdminOperations;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private ObjectProvider<net.hwyz.iov.cloud.framework.kafka.properties.TopicProvisioningProperties> provisioning;

    private VmdKafkaTopicProperties topicProperties;
    private VmdActivationFactTopicReadiness readiness;
    private VmdActivationFactTopicProbe probe;

    @BeforeEach
    void setUp() {
        topicProperties = new VmdKafkaTopicProperties();
        readiness = new VmdActivationFactTopicReadiness();
        probe = new VmdActivationFactTopicProbe(admin, topicProperties,
                new VmdActivationFactConsumerProperties(), readiness, eventPublisher,
                kafkaAdminOperations, provisioning);
    }

    private TopicDescription description(int partitionCount) {
        List<TopicPartitionInfo> partitions = java.util.stream.IntStream.range(0, partitionCount)
                .mapToObj(i -> new TopicPartitionInfo(i, null, List.of(), List.of()))
                .toList();
        return new TopicDescription(TOPIC, false, partitions);
    }

    @Test
    @DisplayName("Topic 存在 → UP + 分区数，并发布探针事件")
    void present_markedUp() throws Exception {
        when(kafkaAdminOperations.describeTopics(any(), anyCollection(), any(Duration.class)))
                .thenReturn(Map.of(TOPIC, description(6)));

        probe.probe();

        assertEquals(VmdActivationFactTopicReadiness.State.UP, readiness.state());
        assertEquals(6, readiness.partitions());
        verify(eventPublisher).publishEvent(new VmdActivationFactTopicProbeEvent(
                VmdActivationFactTopicReadiness.State.UP, 6));
    }

    @Test
    @DisplayName("Topic 缺失（E2E 未建）→ NOT_PRESENT，不抛异常、不阻塞启动")
    void missing_markedNotPresent() throws Exception {
        when(kafkaAdminOperations.describeTopics(any(), anyCollection(), any(Duration.class)))
                .thenThrow(new UnknownTopicOrPartitionException("missing"));

        assertDoesNotThrow(probe::probe);
        assertEquals(VmdActivationFactTopicReadiness.State.NOT_PRESENT, readiness.state());
        verify(eventPublisher).publishEvent(new VmdActivationFactTopicProbeEvent(
                VmdActivationFactTopicReadiness.State.NOT_PRESENT, 0));
    }

    @Test
    @DisplayName("ACL 不足 → DOWN（真实故障，健康上报）")
    void unauthorized_markedDown() throws Exception {
        when(kafkaAdminOperations.describeTopics(any(), anyCollection(), any(Duration.class)))
                .thenThrow(new TopicAuthorizationException("denied"));

        probe.probe();

        assertEquals(VmdActivationFactTopicReadiness.State.DOWN, readiness.state());
        assertTrueReasonContains("权限");
    }

    @Test
    @DisplayName("未知异常（Broker 不可达等）→ DOWN，但不抛出")
    void unexpectedError_markedDownWithoutThrowing() throws Exception {
        when(kafkaAdminOperations.describeTopics(any(), anyCollection(), any(Duration.class)))
                .thenThrow(new IllegalStateException("broker unreachable"));

        assertDoesNotThrow(probe::probe);
        assertEquals(VmdActivationFactTopicReadiness.State.DOWN, readiness.state());
        verify(eventPublisher).publishEvent(new VmdActivationFactTopicProbeEvent(
                VmdActivationFactTopicReadiness.State.DOWN, 0));
    }

    @Test
    @DisplayName("已 UP 时后台重探直接返回（无额外 Admin 调用）")
    void retryProbe_skipsWhenUp() {
        readiness.markUp(6);
        probe.retryProbe();
        org.mockito.Mockito.verifyNoInteractions(kafkaAdminOperations);
    }

    private void assertTrueReasonContains(String keyword) {
        String reason = readiness.reason();
        org.junit.jupiter.api.Assertions.assertNotNull(reason);
        org.junit.jupiter.api.Assertions.assertTrue(reason.contains(keyword),
                "原因应包含[" + keyword + "]，实际=" + reason);
    }

}
