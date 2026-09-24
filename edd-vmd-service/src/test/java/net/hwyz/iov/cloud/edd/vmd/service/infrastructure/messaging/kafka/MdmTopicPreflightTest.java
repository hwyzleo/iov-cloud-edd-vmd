package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.MdmConsumerProperties;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.VmdKafkaTopicProperties;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.DescribeConsumerGroupsResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.errors.TopicAuthorizationException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MDM 消费 Topic 启动预检单元测试（VMD-DSN-CR-052 §5）
 * <p>
 * 验证 11 个 CONSUMER_ONLY Spec、全部存在→UP、单个缺失 / Read 拒绝 / Broker 超时→DOWN、
 * 降级启动→DEGRADED、后台重试探测，以及预检永不调用 Topic 创建/修改 API。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MdmTopicPreflight 测试")
class MdmTopicPreflightTest {

    private static final String GROUP = "iov-cloud-edd-vmd";

    @Mock
    private Admin admin;

    @Mock
    private ObjectProvider<MdmConsumerMetrics> metricsProvider;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private VmdKafkaTopicProperties topicProperties;
    private MdmConsumerProperties consumerProperties;
    private MdmProjectionReadiness readiness;
    private MdmTopicPreflight preflight;
    private ListOffsetsResult offsetsResult;

    @BeforeEach
    void setUp() {
        topicProperties = new VmdKafkaTopicProperties();
        consumerProperties = new MdmConsumerProperties();
        readiness = new MdmProjectionReadiness();
        preflight = new MdmTopicPreflight(admin, topicProperties, readiness, consumerProperties,
                eventPublisher, metricsProvider);
        ReflectionTestUtils.setField(preflight, "consumerGroup", GROUP);
    }

    private TopicDescription topicDescription(String name) {
        List<TopicPartitionInfo> partitions = new ArrayList<>();
        partitions.add(new TopicPartitionInfo(0, new Node(0, "broker-0", 9092),
                List.of(new Node(0, "broker-0", 9092)), List.of(new Node(0, "broker-0", 9092))));
        return new TopicDescription(name, false, partitions);
    }

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
     * listOffsets partitionResult 成功返回（offset 0）。
     */
    private KafkaFuture<ListOffsetsResult.ListOffsetsResultInfo> completedOffset() {
        return KafkaFuture.completedFuture(
                new ListOffsetsResult.ListOffsetsResultInfo(0L, 0L, java.util.Optional.empty()));
    }

    /**
     * 全部 11 个 Topic 存在且可读、消费组可描述。
     */
    private void stubAllPresent() {
        Map<String, KafkaFuture<TopicDescription>> futures = new HashMap<>();
        for (MdmProjectionType p : MdmProjectionType.values()) {
            futures.put(p.directoryTopic(), KafkaFuture.completedFuture(topicDescription(p.directoryTopic())));
        }
        DescribeTopicsResult describeResult = mock(DescribeTopicsResult.class);
        when(describeResult.values()).thenReturn(futures);
        when(admin.describeTopics(anyCollection())).thenReturn(describeResult);

        ListOffsetsResult offsetsResult = mock(ListOffsetsResult.class);
        when(offsetsResult.partitionResult(any(TopicPartition.class))).thenReturn(completedOffset());
        when(admin.listOffsets(anyMap())).thenReturn(offsetsResult);
        this.offsetsResult = offsetsResult;

        DescribeConsumerGroupsResult groupsResult = mock(DescribeConsumerGroupsResult.class);
        when(groupsResult.describedGroups()).thenReturn(Map.of(GROUP,
                KafkaFuture.completedFuture(mock(ConsumerGroupDescription.class))));
        when(admin.describeConsumerGroups(anyCollection())).thenReturn(groupsResult);
    }

    /**
     * 指定单个投影的 describe 失败（携带异常）。
     */
    private void stubDescribeFailure(MdmProjectionType target, Throwable cause) {
        Map<String, KafkaFuture<TopicDescription>> futures = new HashMap<>();
        for (MdmProjectionType p : MdmProjectionType.values()) {
            if (p == target) {
                futures.put(p.directoryTopic(), failedFuture(cause));
            } else {
                futures.put(p.directoryTopic(), KafkaFuture.completedFuture(topicDescription(p.directoryTopic())));
            }
        }
        DescribeTopicsResult describeResult = mock(DescribeTopicsResult.class);
        when(describeResult.values()).thenReturn(futures);
        when(admin.describeTopics(anyCollection())).thenReturn(describeResult);
    }

    private MdmProjectionPreflightResultEvent captureEvent() {
        ArgumentCaptor<MdmProjectionPreflightResultEvent> captor =
                ArgumentCaptor.forClass(MdmProjectionPreflightResultEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("构建 11 个 CONSUMER_ONLY Spec，消费组与目录一致")
    void buildSpecs_allConsumerOnly() {
        List<MdmConsumerTopicSpec> specs = preflight.buildSpecs();
        assertEquals(11, specs.size());
        for (MdmConsumerTopicSpec spec : specs) {
            assertEquals(VmdKafkaTopicRole.CONSUMER_ONLY, spec.ownership());
            assertTrue(spec.required());
            assertEquals(GROUP, spec.consumerGroup());
            assertEquals(topicProperties.topic(spec.projectionType()), spec.topicName());
        }
    }

    @Test
    @DisplayName("全部存在且可访问：11 投影 UP，发布 allPassed 事件")
    void allPresent_readinessUp() {
        stubAllPresent();

        preflight.preflight();

        assertTrue(readiness.allReady());
        MdmProjectionPreflightResultEvent event = captureEvent();
        assertTrue(event.allPassed());
        assertEquals(11, event.results().size());
        for (MdmProjectionType p : MdmProjectionType.values()) {
            assertTrue(event.results().get(p).ok());
        }
    }

    @Test
    @DisplayName("单个 Topic 缺失：该投影 DOWN（fail-fast），不创建/不修改，事件 allPassed=false")
    void singleMissing_readinessDown_noCreate() {
        stubDescribeFailure(MdmProjectionType.BRAND, new UnknownTopicOrPartitionException("no such topic"));

        preflight.preflight();

        assertFalse(readiness.allReady());
        assertEquals(MdmProjectionReadiness.State.DOWN, readiness.status(MdmProjectionType.BRAND).state());
        assertFalse(readiness.status(MdmProjectionType.BRAND).topicReachable());
        MdmProjectionPreflightResultEvent event = captureEvent();
        assertFalse(event.allPassed());
        assertEquals(MdmPreflightClassification.MISSING, event.results().get(MdmProjectionType.BRAND).classification());
        // 预检永不创建 / 修改上游 Topic
        verify(admin, never()).createTopics(anyCollection());
        verify(admin, never()).createPartitions(anyMap());
        verify(admin, never()).alterConfigs(anyMap());
    }

    @Test
    @DisplayName("Read ACL 拒绝：分类 UNAUTHORIZED，投影 DOWN")
    void readDenied_unauthorizedDown() {
        stubAllPresent();
        KafkaFuture<ListOffsetsResult.ListOffsetsResultInfo> denied =
                failedFuture(new TopicAuthorizationException("not authorized to read"));
        when(offsetsResult.partitionResult(new TopicPartition("mdm.platform", 0))).thenReturn(denied);

        preflight.preflight();

        assertEquals(MdmProjectionReadiness.State.DOWN, readiness.status(MdmProjectionType.PLATFORM).state());
        MdmProjectionPreflightResultEvent event = captureEvent();
        assertEquals(MdmPreflightClassification.UNAUTHORIZED, event.results().get(MdmProjectionType.PLATFORM).classification());
        // 失败原因不含凭证
        String reason = readiness.status(MdmProjectionType.PLATFORM).reason();
        assertNotNull(reason);
        assertFalse(reason.toLowerCase().contains("credential"));
        assertFalse(reason.toLowerCase().contains("password"));
        verify(admin, never()).createTopics(anyCollection());
    }

    @Test
    @DisplayName("Broker 超时：分类 TIMEOUT，投影 DOWN")
    void brokerTimeout_timeoutDown() {
        stubDescribeFailure(MdmProjectionType.CONFIGURATION,
                new org.apache.kafka.common.errors.TimeoutException("timeout"));

        preflight.preflight();

        assertEquals(MdmProjectionReadiness.State.DOWN, readiness.status(MdmProjectionType.CONFIGURATION).state());
        MdmProjectionPreflightResultEvent event = captureEvent();
        assertEquals(MdmPreflightClassification.TIMEOUT, event.results().get(MdmProjectionType.CONFIGURATION).classification());
    }

    @Test
    @DisplayName("降级启动（fail-fast=false）：投影 DEGRADED，仍不创建 Topic")
    void degradedMode_degradedState() {
        consumerProperties.setFailFast(false);
        stubDescribeFailure(MdmProjectionType.PART, new UnknownTopicOrPartitionException("no such topic"));

        preflight.preflight();

        assertEquals(MdmProjectionReadiness.State.DEGRADED, readiness.status(MdmProjectionType.PART).state());
        assertTrue(readiness.anyFailure());
        verify(admin, never()).createTopics(anyCollection());
        verify(admin, never()).alterConfigs(anyMap());
    }

    @Test
    @DisplayName("后台重试：预检失败后继续探测，全部通过后停止")
    void retryProbe_untilReady() {
        consumerProperties.setFailFast(false);
        stubDescribeFailure(MdmProjectionType.PART, new UnknownTopicOrPartitionException("no such topic"));

        preflight.preflight();
        assertFalse(readiness.allReady());

        // 恢复后重试探测
        stubAllPresent();
        preflight.retryProbe();
        assertTrue(readiness.allReady());

        // 已全部通过：再触发不执行（无副作用）
        int before = readiness.statuses().size();
        preflight.retryProbe();
        assertEquals(before, readiness.statuses().size());
        assertEquals(11, readiness.statuses().size());
    }

    @Test
    @DisplayName("预检未执行（started=false）：重试探测不执行")
    void retryProbe_notStarted_skips() {
        preflight.retryProbe();
        verify(admin, never()).describeTopics(anyCollection());
    }
}
