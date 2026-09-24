package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.VmdKafkaTopicProperties;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.VmdKafkaTopicProvisioningProperties;
import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicsReadyEvent;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.DescribeConfigsResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * VMD Kafka Topic 初始化器单元测试（VMD-DSN-CR-051）
 * <p>
 * 验证既有校验（分区数 / 副本数 / cleanup.policy）漂移仅上报不自动修改、
 * readiness 置 DOWN、观测消费 Topic 独立探测且不创建。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VmdKafkaTopicInitializer 测试")
class VmdKafkaTopicInitializerTest {

    private static final String BINDING = "vmd.vehcile-part-binding.changed";
    private static final String PRODUCE = "vmd.vehicle-produce";
    private static final String SW_INVENTORY = "vmd.vehicle-software-inventory.changed";
    private static final String OBSERVED = "ota.vehicle-software-inventory.observed";

    @Mock
    private Admin admin;

    @Mock
    private ObjectProvider<VmdKafkaTopicMetrics> metricsProvider;

    @Mock
    private MdmTopicPreflight mdmTopicPreflight;

    private VmdKafkaTopicProperties topicProperties;
    private VmdKafkaTopicProvisioningProperties provisioningProperties;
    private VmdKafkaTopicReadiness readiness;
    private VmdKafkaTopicInitializer initializer;
    private DescribeTopicsResult producerDescribeResult;

    @BeforeEach
    void setUp() {
        topicProperties = new VmdKafkaTopicProperties();
        provisioningProperties = new VmdKafkaTopicProvisioningProperties();
        provisioningProperties.setPartitions(3);
        provisioningProperties.setReplicationFactor((short) 3);
        provisioningProperties.setCleanupPolicy("delete");
        readiness = new VmdKafkaTopicReadiness();
        initializer = new VmdKafkaTopicInitializer(
                admin, topicProperties, provisioningProperties, readiness, metricsProvider, mdmTopicPreflight);
    }

    private TopicDescription topicDescription(String name, int partitions, int replication) {
        List<TopicPartitionInfo> partitionInfos = new ArrayList<>();
        for (int i = 0; i < partitions; i++) {
            partitionInfos.add(new TopicPartitionInfo(i,
                    new Node(0, "localhost", 9092),
                    replicationReplicas(replication),
                    replicationReplicas(replication)));
        }
        return new TopicDescription(name, false, partitionInfos);
    }

    private List<Node> replicationReplicas(int replication) {
        ArrayList<Node> nodes = new ArrayList<>();
        for (int i = 0; i < replication; i++) {
            nodes.add(new Node(i, "broker-" + i, 9092));
        }
        return nodes;
    }

    private Config config(String cleanupPolicy) {
        return new Config(List.of(new ConfigEntry("cleanup.policy", cleanupPolicy)));
    }

    /**
     * 构造 get() 抛 ExecutionException 的失败 KafkaFuture（kafka-clients 3.3.2 无 failedFuture）。
     */
    @SuppressWarnings("unchecked")
    private <T> KafkaFuture<T> failedFuture(Throwable cause) {
        KafkaFuture<T> future = mock(KafkaFuture.class);
        try {
            lenient().when(future.get(anyLong(), any(TimeUnit.class))).thenThrow(new ExecutionException(cause));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return future;
    }

    /**
     * 三个生产 Topic 均存在且配置匹配，并 stub describeConfigs。
     */
    private void stubProducerDescribe(String cleanupPolicy) {
        KafkaFuture<TopicDescription> binding = KafkaFuture.completedFuture(topicDescription(BINDING, 3, 3));
        KafkaFuture<TopicDescription> produce = KafkaFuture.completedFuture(topicDescription(PRODUCE, 3, 3));
        KafkaFuture<TopicDescription> sw = KafkaFuture.completedFuture(topicDescription(SW_INVENTORY, 3, 3));
        producerDescribeResult = mock(DescribeTopicsResult.class);
        when(producerDescribeResult.values()).thenReturn(Map.of(
                BINDING, binding, PRODUCE, produce, SW_INVENTORY, sw));
        when(admin.describeTopics(anyCollection())).thenReturn(producerDescribeResult);

        Map<ConfigResource, Config> configs = Map.of(
                new ConfigResource(ConfigResource.Type.TOPIC, BINDING), config(cleanupPolicy),
                new ConfigResource(ConfigResource.Type.TOPIC, PRODUCE), config(cleanupPolicy),
                new ConfigResource(ConfigResource.Type.TOPIC, SW_INVENTORY), config(cleanupPolicy));
        DescribeConfigsResult configResult = mock(DescribeConfigsResult.class);
        when(configResult.all()).thenReturn(KafkaFuture.completedFuture(configs));
        when(admin.describeConfigs(anyCollection())).thenReturn(configResult);
    }

    /**
     * 生产 describe 返回 producerDescribeResult，消费 describe 返回可访问结果（顺序 stub）。
     */
    private void stubConsumerPresent() {
        KafkaFuture<TopicDescription> observed = KafkaFuture.completedFuture(topicDescription(OBSERVED, 3, 3));
        DescribeTopicsResult consumerResult = mock(DescribeTopicsResult.class);
        when(consumerResult.values()).thenReturn(Map.of(OBSERVED, observed));
        when(admin.describeTopics(anyCollection()))
                .thenReturn(producerDescribeResult)
                .thenReturn(consumerResult);
    }

    private void stubConsumerMissing() {
        KafkaFuture<TopicDescription> missing = failedFuture(new UnknownTopicOrPartitionException("no such topic"));
        DescribeTopicsResult consumerResult = mock(DescribeTopicsResult.class);
        when(consumerResult.values()).thenReturn(Map.of(OBSERVED, missing));
        when(admin.describeTopics(anyCollection()))
                .thenReturn(producerDescribeResult)
                .thenReturn(consumerResult);
    }

    @Test
    @DisplayName("三生产 Topic 与消费 Topic 均匹配时 readiness=UP")
    void allMatch_readinessUp() {
        stubProducerDescribe("delete");
        stubConsumerPresent();

        initializer.onTopicsReady(new KafkaTopicsReadyEvent());

        assertEquals(VmdKafkaTopicReadiness.State.UP, readiness.producerTopicsState());
        assertTrue(readiness.producerMismatches().isEmpty());
        assertEquals(VmdKafkaTopicReadiness.State.UP, readiness.inventoryObservedState());
    }

    @Test
    @DisplayName("分区数小于期望：上报 PARTITIONS 漂移，readiness=DOWN，不自动修改")
    void partitionsShort_readinessDown_noAutoFix() {
        KafkaFuture<TopicDescription> binding = KafkaFuture.completedFuture(topicDescription(BINDING, 2, 3));
        KafkaFuture<TopicDescription> produce = KafkaFuture.completedFuture(topicDescription(PRODUCE, 3, 3));
        KafkaFuture<TopicDescription> sw = KafkaFuture.completedFuture(topicDescription(SW_INVENTORY, 3, 3));
        producerDescribeResult = mock(DescribeTopicsResult.class);
        when(producerDescribeResult.values()).thenReturn(Map.of(
                BINDING, binding, PRODUCE, produce, SW_INVENTORY, sw));
        when(admin.describeTopics(anyCollection())).thenReturn(producerDescribeResult);

        Map<ConfigResource, Config> configs = Map.of(
                new ConfigResource(ConfigResource.Type.TOPIC, BINDING), config("delete"),
                new ConfigResource(ConfigResource.Type.TOPIC, PRODUCE), config("delete"),
                new ConfigResource(ConfigResource.Type.TOPIC, SW_INVENTORY), config("delete"));
        DescribeConfigsResult configResult = mock(DescribeConfigsResult.class);
        when(configResult.all()).thenReturn(KafkaFuture.completedFuture(configs));
        when(admin.describeConfigs(anyCollection())).thenReturn(configResult);

        stubConsumerPresent();

        initializer.onTopicsReady(new KafkaTopicsReadyEvent());

        assertEquals(VmdKafkaTopicReadiness.State.DOWN, readiness.producerTopicsState());
        assertTrue(readiness.producerMismatches().stream()
                        .anyMatch(m -> BINDING.equals(m.topic()) && "PARTITIONS".equals(m.type())),
                "应上报 PARTITIONS 漂移");
        // 漂移不得触发任何创建/变更
        verify(admin, never()).createTopics(anyCollection());
        verify(admin, never()).alterConfigs(anyMap());
    }

    @Test
    @DisplayName("副本数与 cleanup.policy 不符：上报漂移，readiness=DOWN")
    void replicationAndConfigMismatch_readinessDown() {
        KafkaFuture<TopicDescription> binding = KafkaFuture.completedFuture(topicDescription(BINDING, 3, 1));
        KafkaFuture<TopicDescription> produce = KafkaFuture.completedFuture(topicDescription(PRODUCE, 3, 3));
        KafkaFuture<TopicDescription> sw = KafkaFuture.completedFuture(topicDescription(SW_INVENTORY, 3, 3));
        producerDescribeResult = mock(DescribeTopicsResult.class);
        when(producerDescribeResult.values()).thenReturn(Map.of(
                BINDING, binding, PRODUCE, produce, SW_INVENTORY, sw));
        when(admin.describeTopics(anyCollection())).thenReturn(producerDescribeResult);

        Map<ConfigResource, Config> configs = Map.of(
                new ConfigResource(ConfigResource.Type.TOPIC, BINDING), config("compact"),
                new ConfigResource(ConfigResource.Type.TOPIC, PRODUCE), config("delete"),
                new ConfigResource(ConfigResource.Type.TOPIC, SW_INVENTORY), config("delete"));
        DescribeConfigsResult configResult = mock(DescribeConfigsResult.class);
        when(configResult.all()).thenReturn(KafkaFuture.completedFuture(configs));
        when(admin.describeConfigs(anyCollection())).thenReturn(configResult);

        stubConsumerPresent();

        initializer.onTopicsReady(new KafkaTopicsReadyEvent());

        assertEquals(VmdKafkaTopicReadiness.State.DOWN, readiness.producerTopicsState());
        assertTrue(readiness.producerMismatches().stream()
                        .anyMatch(m -> BINDING.equals(m.topic()) && "REPLICATION_FACTOR".equals(m.type())),
                "应上报 REPLICATION_FACTOR 漂移");
        assertTrue(readiness.producerMismatches().stream()
                        .anyMatch(m -> BINDING.equals(m.topic()) && m.type().startsWith("CONFIG_")),
                "应上报 CONFIG 漂移");
        verify(admin, never()).createTopics(anyCollection());
    }

    @Test
    @DisplayName("消费 Topic 暂不存在：独立 NOT_PRESENT 健康项，不置生产 DOWN，不创建")
    void consumerMissing_notDown_notCreated() {
        stubProducerDescribe("delete");
        stubConsumerMissing();

        initializer.onTopicsReady(new KafkaTopicsReadyEvent());

        assertEquals(VmdKafkaTopicReadiness.State.UP, readiness.producerTopicsState(),
                "消费 Topic 不存在不应影响生产 Topic readiness");
        assertEquals(VmdKafkaTopicReadiness.State.NOT_PRESENT, readiness.inventoryObservedState());
        verify(admin, never()).createTopics(anyCollection());
    }

    @Test
    @DisplayName("describe 异常（Broker/权限）：生产 readiness=DOWN 并记录原因")
    void describeError_readinessDown() {
        KafkaFuture<TopicDescription> binding = failedFuture(new TimeoutException("timeout"));
        KafkaFuture<TopicDescription> produce = failedFuture(new TimeoutException("timeout"));
        KafkaFuture<TopicDescription> sw = failedFuture(new TimeoutException("timeout"));
        DescribeTopicsResult result = mock(DescribeTopicsResult.class);
        when(result.values()).thenReturn(Map.of(BINDING, binding, PRODUCE, produce, SW_INVENTORY, sw));
        when(admin.describeTopics(anyCollection())).thenReturn(result);

        initializer.onTopicsReady(new KafkaTopicsReadyEvent());

        assertEquals(VmdKafkaTopicReadiness.State.DOWN, readiness.producerTopicsState());
        assertTrue(readiness.producerReason() != null && readiness.producerReason().contains("既有校验失败"));
    }

    @Test
    @DisplayName("初始化结果按 Topic 记录指标")
    void recordsInitMetrics() {
        VmdKafkaTopicMetrics metrics = mock(VmdKafkaTopicMetrics.class);
        when(metricsProvider.getIfAvailable()).thenReturn(metrics);
        stubProducerDescribe("delete");
        stubConsumerPresent();

        initializer.onTopicsReady(new KafkaTopicsReadyEvent());

        verify(metrics).recordInit(BINDING, "ok");
        verify(metrics).recordInit(PRODUCE, "ok");
        verify(metrics).recordInit(SW_INVENTORY, "ok");
        verify(metrics).recordInit(OBSERVED, "present");
    }
}
