package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.MdmConsumerProperties;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.VmdKafkaTopicProperties;
import net.hwyz.iov.cloud.framework.kafka.properties.TopicProvisioningProperties;
import net.hwyz.iov.cloud.framework.kafka.support.KafkaAdminOperations;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.errors.ClusterAuthorizationException;
import org.apache.kafka.common.errors.GroupAuthorizationException;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.TopicAuthorizationException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MDM 消费 Topic 启动预检（VMD-DSN-CR-052 §5 / F18，RD-052-2）
 * <p>
 * 对 11 个 CONSUMER_ONLY MDM Topic 执行存在性与访问权限预检：
 * <ol>
 *   <li>构建 11 个 {@link MdmConsumerTopicSpec}（ownership=CONSUMER_ONLY）</li>
 *   <li>describeTopics / metadata 探测存在性与 Describe 权限</li>
 *   <li>listOffsets（partition 0）验证 Read 权限；describeConsumerGroups 验证消费组权限</li>
 *   <li>逐投影输出逻辑名 / 实际 Topic / 投影类型 / 错误分类 / 处置建议（不记录凭证），
 *       写入 {@link MdmProjectionReadiness} 并发布 {@link MdmProjectionPreflightResultEvent}</li>
 * </ol>
 * 约束：
 * <ul>
 *   <li>MDM Spec 永不进入 createTopics / createPartitions / alterConfigs 集合</li>
 *   <li>生产默认 fail-fast=true；降级启动（fail-fast=false）时健康仍为 DOWN/DEGRADED，
 *       且后台按 {@code vmd.kafka.mdm-consumer.retry-interval-ms} 持续重试探测</li>
 *   <li>预检只证明 Topic 可访问，不代表投影数据已同步；Bootstrap 与增量消费状态分开暴露</li>
 * </ul>
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MdmTopicPreflight {

    private static final Duration DEFAULT_DESCRIBE_TIMEOUT = Duration.ofSeconds(10);
    private static final int DEFAULT_WARMUP_ATTEMPTS = 3;
    private static final Duration DEFAULT_WARMUP_BACKOFF = Duration.ofSeconds(1);

    private final Admin admin;
    private final VmdKafkaTopicProperties topicProperties;
    private final MdmProjectionReadiness readiness;
    private final MdmConsumerProperties consumerProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectProvider<MdmConsumerMetrics> metricsProvider;
    private final KafkaAdminOperations kafkaAdminOperations;
    private final ObjectProvider<TopicProvisioningProperties> frameworkTopicProvisioningProperties;

    /**
     * 消费组（沿用既有稳定业务组名，Topic 更名不通过修改 Group ID 模拟 Offset 迁移）。
     */
    @Value("${spring.kafka.consumer.group-id:iov-cloud-edd-vmd}")
    private String consumerGroup;

    private final AtomicBoolean started = new AtomicBoolean(false);

    /**
     * 执行 11 个 MDM 消费 Topic 启动预检。
     */
    public void preflight() {
        started.set(true);
        if (!warmUpAdmin()) {
            log.warn("Kafka Admin 连接预热未达成，本轮跳过 MDM 消费 Topic 预检（后台将按 retry-interval 重试）");
            return;
        }
        List<MdmConsumerTopicSpec> specs = buildSpecs();
        Map<MdmProjectionType, MdmPreflightResult> results = new LinkedHashMap<>();
        for (MdmConsumerTopicSpec spec : specs) {
            MdmPreflightResult result = checkOne(spec);
            results.put(spec.projectionType(), result);
            applyToReadiness(spec, result);
            recordMetric(spec, result);
            logResult(spec, result);
        }

        boolean allPassed = readiness.allReady();
        eventPublisher.publishEvent(new MdmProjectionPreflightResultEvent(specs, results, allPassed));

        if (allPassed) {
            log.info("MDM 消费 Topic 预检全部通过（11/11）：{}",
                    specs.stream().map(MdmConsumerTopicSpec::topicName).toList());
        } else {
            log.error("MDM 消费 Topic 预检未全部通过（fail-fast={}），健康状态保持 DOWN/DEGRADED；"
                            + "后台将按 {}ms 持续重试探测",
                    consumerProperties.isFailFast(), consumerProperties.getRetryIntervalMs());
        }
    }

    /**
     * 后台重试探测：仅在预检已执行且未全部通过时继续探测。
     */
    @Scheduled(fixedDelayString = "${vmd.kafka.mdm-consumer.retry-interval-ms:60000}")
    public void retryProbe() {
        if (!started.get() || readiness.allReady()) {
            return;
        }
        log.warn("MDM 消费 Topic 预检未通过，后台重试探测（仅预检，不创建/修改上游 Topic）");
        preflight();
    }

    /**
     * 构建 11 个 CONSUMER_ONLY Topic Spec。
     */
    public List<MdmConsumerTopicSpec> buildSpecs() {
        List<MdmConsumerTopicSpec> specs = new ArrayList<>(MdmProjectionType.values().length);
        for (MdmProjectionType projection : MdmProjectionType.values()) {
            specs.add(new MdmConsumerTopicSpec(
                    projection.configKey(),
                    topicProperties.topic(projection),
                    projection,
                    true,
                    consumerGroup,
                    VmdKafkaTopicRole.CONSUMER_ONLY));
        }
        return specs;
    }

    /**
     * 单投影预检：存在性 + Describe + Read（listOffsets）+ 消费组权限。
     */
    private MdmPreflightResult checkOne(MdmConsumerTopicSpec spec) {
        try {
            TopicDescription description = describeTopic(spec.topicName());
            checkReadAccess(spec.topicName(), description);
            checkConsumerGroup(spec.consumerGroup());
            return MdmPreflightResult.ok(spec);
        } catch (MdmPreflightException e) {
            log.warn("MDM 消费 Topic 预检失败: logicalName={}, topic={}, projection={}, classification={}, reason={}, disposition={}",
                    spec.logicalName(), spec.topicName(), spec.projectionType().configKey(),
                    e.classification().tag(), e.getMessage(), e.classification().disposition());
            return MdmPreflightResult.failed(spec, e.classification(), e.getMessage());
        } catch (Exception e) {
            log.warn("MDM 消费 Topic 预检异常: logicalName={}, topic={}, projection={}, error={}",
                    spec.logicalName(), spec.topicName(), spec.projectionType().configKey(), e.getMessage());
            return MdmPreflightResult.failed(spec, MdmPreflightClassification.ERROR, safeMessage(e));
        }
    }

    private TopicDescription describeTopic(String topic) {
        try {
            return kafkaAdminOperations.describeTopics(admin, List.of(topic), describeTimeout()).get(topic);
        } catch (TimeoutException | org.apache.kafka.common.errors.TimeoutException e) {
            throw classify(e);
        } catch (RuntimeException e) {
            throw classify(e);
        }
    }

    /**
     * describe 共享等待预算：优先取 FW-KAFKA iov.kafka.topic-provisioning.describe-timeout，
     * 未启用 Provisioning 的上下文回退框架默认 10s。
     */
    private Duration describeTimeout() {
        TopicProvisioningProperties properties = frameworkTopicProvisioningProperties.getIfAvailable();
        return properties == null ? DEFAULT_DESCRIBE_TIMEOUT : properties.describeTimeout();
    }

    /**
     * 启动期 Admin 连接预热（FW-KAFKA KafkaAdminOperations），参数解析同 describeTimeout。
     */
    private boolean warmUpAdmin() {
        TopicProvisioningProperties properties = frameworkTopicProvisioningProperties.getIfAvailable();
        if (properties == null) {
            return kafkaAdminOperations.warmUp(admin, DEFAULT_DESCRIBE_TIMEOUT,
                    DEFAULT_WARMUP_ATTEMPTS, DEFAULT_WARMUP_BACKOFF);
        }
        TopicProvisioningProperties.Warmup warmup = properties.warmup();
        return kafkaAdminOperations.warmUp(admin, properties.describeTimeout(),
                warmup.attempts(), warmup.initialBackoff());
    }

    /**
     * Read 权限探测：对 partition 0 执行 earliest offset 读取。
     * <p>Topic 缺失（UnknownTopicOrPartitionException）与 Read 权限不足
     * （TopicAuthorizationException）在此被分类，缺失 Topic 不触发创建。
     */
    private void checkReadAccess(String topic, TopicDescription description) {
        if (description == null || description.partitions() == null || description.partitions().isEmpty()) {
            log.warn("MDM 消费 Topic 无分区信息（跳过 Read 探测）: topic={}", topic);
            return;
        }
        TopicPartition partition = new TopicPartition(topic, 0);
        ListOffsetsResult result = admin.listOffsets(Map.of(partition, OffsetSpec.earliest()));
        try {
            result.partitionResult(partition).get(describeTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (ExecutionException | TimeoutException | org.apache.kafka.common.errors.TimeoutException e) {
            throw classify(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MdmPreflightException(MdmPreflightClassification.ERROR, "预检线程被中断");
        }
    }

    /**
     * 消费组权限探测：describeConsumerGroups。
     * <p>组不存在（GroupIdNotFoundException）为正常状态（Listener 启动后自动建组），
     * 不视为失败；Group 权限不足按 UNAUTHORIZED 失败。
     */
    private void checkConsumerGroup(String group) {
        try {
            KafkaFuture<org.apache.kafka.clients.admin.ConsumerGroupDescription> future =
                    admin.describeConsumerGroups(List.of(group)).describedGroups().get(group);
            future.get(describeTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            Throwable cause = unwrap(e);
            if (cause instanceof GroupIdNotFoundException) {
                log.debug("MDM 消费组暂不存在（Listener 启动后自动创建，不视为失败）: group={}", group);
                return;
            }
            throw classify(e);
        } catch (TimeoutException | org.apache.kafka.common.errors.TimeoutException e) {
            throw classify(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MdmPreflightException(MdmPreflightClassification.ERROR, "预检线程被中断");
        }
    }

    /**
     * 将预检结果写入逐投影 Readiness。
     */
    private void applyToReadiness(MdmConsumerTopicSpec spec, MdmPreflightResult result) {
        if (result.ok()) {
            readiness.markPreflightPassed(spec.projectionType(), spec.topicName());
            return;
        }
        String reason = result.classification().tag() + ": " + result.message();
        if (consumerProperties.isFailFast()) {
            readiness.markPreflightFailed(spec.projectionType(), spec.topicName(), reason);
        } else {
            readiness.markDegraded(spec.projectionType(), spec.topicName(), reason);
        }
    }

    private void recordMetric(MdmConsumerTopicSpec spec, MdmPreflightResult result) {
        MdmConsumerMetrics metrics = metricsProvider == null ? null : metricsProvider.getIfAvailable();
        if (metrics != null) {
            metrics.recordTopicCheck(spec.topicName(), result.resultTag());
        }
    }

    private void logResult(MdmConsumerTopicSpec spec, MdmPreflightResult result) {
        if (result.ok()) {
            log.info("MDM 消费 Topic 预检通过: logicalName={}, topic={}, projection={}, consumerGroup={}",
                    spec.logicalName(), spec.topicName(), spec.projectionType().configKey(), spec.consumerGroup());
        }
    }

    /**
     * 按异常分类为预检错误。
     */
    private MdmPreflightException classify(Throwable ex) {
        Throwable cause = unwrap(ex);
        if (cause instanceof UnknownTopicOrPartitionException) {
            return new MdmPreflightException(MdmPreflightClassification.MISSING, "Topic 不存在（归 EDD-MDM 创建，VMD 不创建）");
        }
        if (cause instanceof TopicAuthorizationException
                || cause instanceof GroupAuthorizationException
                || cause instanceof ClusterAuthorizationException) {
            return new MdmPreflightException(MdmPreflightClassification.UNAUTHORIZED,
                    "Describe/Read 或 ConsumerGroup 权限不足（不记录凭证）");
        }
        if (cause instanceof org.apache.kafka.common.errors.TimeoutException
                || cause instanceof TimeoutException) {
            return new MdmPreflightException(MdmPreflightClassification.TIMEOUT, "Broker 超时或集群不可达");
        }
        return new MdmPreflightException(MdmPreflightClassification.ERROR, safeMessage(cause));
    }

    private Throwable unwrap(Throwable ex) {
        Throwable t = ex;
        while ((t instanceof ExecutionException) && t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }

    private String safeMessage(Throwable t) {
        String message = t.getMessage();
        return message == null || message.isBlank() ? t.getClass().getSimpleName() : message;
    }
}
