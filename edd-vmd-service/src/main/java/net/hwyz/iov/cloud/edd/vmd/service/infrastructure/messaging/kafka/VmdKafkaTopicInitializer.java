package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * VMD Kafka Topic 初始化与既有校验组件（VMD-DSN-CR-051）
 * <p>
 * 复用 FW-KAFKA 的 Provisioning（Catalog 合并 → describe → 缺失幂等创建 → KafkaTopicsReadyEvent），
 * 在 {@link KafkaTopicsReadyEvent} 后完成：
 * <ol>
 *   <li>对三个 PRODUCER_OWNED 生产 Topic 做既有校验（分区数 / 副本数 / cleanup.policy），
 *       漂移仅上报（指标 + 日志）并置 TopicReadiness=DOWN，不自动修改拓扑</li>
 *   <li>对 CONSUMER_ONLY 观测 Topic 做可访问性探测，独立健康项，短时不存在不触发创建</li>
 * </ol>
 * 多实例并发创建幂等由 FW-KAFKA 的 TopicExistsException 处理承担，本组件回读校验。
 * <p>
 * 无 Create 权限 / Broker 不可用由 FW-KAFKA Provisioning 状态（NOT_READY）体现，
 * 健康检查据此报告 DOWN；本组件仅在 Topic 已就绪后执行漂移校验。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "iov.kafka.topic-provisioning", name = "enabled", havingValue = "true")
public class VmdKafkaTopicInitializer {

    private static final long DESCRIBE_TIMEOUT_MS = 5000;

    private final Admin admin;
    private final VmdKafkaTopicProperties topicProperties;
    private final VmdKafkaTopicProvisioningProperties provisioningProperties;
    private final VmdKafkaTopicReadiness readiness;
    private final ObjectProvider<VmdKafkaTopicMetrics> metricsProvider;
    private final MdmTopicPreflight mdmTopicPreflight;

    /**
     * KafkaTopicsReadyEvent 是否已触发（未触发前不执行后台重试）。
     */
    private final AtomicBoolean started = new AtomicBoolean(false);

    /**
     * 启动日志：逻辑名 → 实际 Topic → 角色 → 参数（不打印凭证）。
     */
    @PostConstruct
    public void logTopicCatalog() {
        log.info("VMD Kafka Topic 目录（VMD-DSN-CR-051 / VMD-DSN-CR-052）:");
        for (VmdKafkaLogicalTopic logical : VmdKafkaLogicalTopic.values()) {
            log.info("  logicalName={}, topic={}, role={}, partitions={}, replicationFactor={}, cleanupPolicy={}",
                    logical.configKey(), topicProperties.topic(logical), logical.role(),
                    provisioningProperties.getPartitions(),
                    provisioningProperties.getReplicationFactor(),
                    provisioningProperties.getCleanupPolicy());
        }
        log.info("  MDM 消费 Topic（11，全部 CONSUMER_ONLY，归 EDD-MDM 管理，VMD 只预检不创建）:");
        for (MdmProjectionType projection : MdmProjectionType.values()) {
            log.info("  projection={}, topic={}, consumerId={}",
                    projection.configKey(), topicProperties.topic(projection), projection.consumerId());
        }
    }

    @EventListener
    public void onTopicsReady(KafkaTopicsReadyEvent event) {
        started.set(true);
        log.info("KafkaTopicsReadyEvent 触发 VMD 生产 Topic 既有校验、观测 Topic 探测与 MDM 消费 Topic 预检");
        validateProducerTopics();
        probeInventoryObserved();
        mdmTopicPreflight.preflight();
    }

    /**
     * 就绪未达成时的后台重试（仅校验，不创建/修改 Topic）。
     * <p>
     * 生产 Topic 既有校验与观测 Topic 探测均为一次性执行：若应用启动瞬间
     * AdminClient 与 Broker 的连接尚未就绪（DNS / 双栈 / advertised listener
     * 预热等）而超时失败，readiness 会被置 DOWN 且永不恢复，导致 Outbox Relay
     * 永久暂停。此处仿照 {@link MdmTopicPreflight#retryProbe()} 增加定时自愈：
     * 生产 Topic 未达 UP、或观测 Topic 处于 DOWN（瞬态失败）时周期重试。
     */
    @Scheduled(fixedDelayString = "${vmd.kafka.topic-provisioning.retry-interval-ms:60000}")
    public void retryValidate() {
        if (!started.get()) {
            return;
        }
        boolean producerNotReady = readiness.producerTopicsState() != VmdKafkaTopicReadiness.State.UP;
        boolean observedDown = readiness.inventoryObservedState() == VmdKafkaTopicReadiness.State.DOWN;
        if (!producerNotReady && !observedDown) {
            return;
        }
        log.warn("VMD 生产/观测 Topic 就绪未达成，后台重试既有校验与探测（仅校验，不创建/修改 Topic）");
        if (producerNotReady) {
            validateProducerTopics();
        }
        if (observedDown) {
            probeInventoryObserved();
        }
    }

    /**
     * 构造三个 PRODUCER_OWNED Topic 规格。
     */
    private List<VmdKafkaTopicSpec> producerSpecs() {
        return List.of(
                spec(VmdKafkaLogicalTopic.PART_BINDING_CHANGED),
                spec(VmdKafkaLogicalTopic.VEHICLE_PRODUCE),
                spec(VmdKafkaLogicalTopic.SOFTWARE_INVENTORY_CHANGED));
    }

    private VmdKafkaTopicSpec spec(VmdKafkaLogicalTopic logical) {
        return new VmdKafkaTopicSpec(
                logical,
                topicProperties.topic(logical),
                VmdKafkaTopicRole.PRODUCER_OWNED,
                provisioningProperties.getPartitions(),
                provisioningProperties.getReplicationFactor(),
                provisioningProperties.configs());
    }

    /**
     * 对三个生产 Topic 执行既有校验；漂移仅上报，不自动修改。
     */
    private void validateProducerTopics() {
        List<VmdKafkaTopicSpec> specs = producerSpecs();
        Map<String, TopicDescription> descriptions;
        Map<ConfigResource, Config> configs;
        try {
            descriptions = describe(specs.stream().map(VmdKafkaTopicSpec::topicName).toList());
            configs = describeConfigs(specs);
        } catch (Exception e) {
            readiness.markProducerTopicsDown(List.of(), "生产 Topic 既有校验失败: " + e.getMessage());
            specs.forEach(spec -> recordInit(spec.topicName(), "error"));
            log.error("VMD 生产 Topic 既有校验失败: {}", e.getMessage(), e);
            return;
        }

        List<VmdKafkaTopicReadiness.TopicConfigMismatch> mismatches = new ArrayList<>();
        for (VmdKafkaTopicSpec spec : specs) {
            TopicDescription description = descriptions.get(spec.topicName());
            if (description == null) {
                mismatches.add(new VmdKafkaTopicReadiness.TopicConfigMismatch(
                        spec.topicName(), "MISSING", "存在", "不存在"));
                recordInit(spec.topicName(), "down");
                log.error("VMD 生产 Topic 不存在: logicalName={}, topic={}", spec.logicalName().configKey(), spec.topicName());
                continue;
            }
            validateOne(spec, description, configs, mismatches);
        }

        if (mismatches.isEmpty()) {
            readiness.markProducerTopicsUp();
            log.info("VMD 生产 Topic 既有校验通过: {}",
                    specs.stream().map(VmdKafkaTopicSpec::topicName).toList());
        } else {
            readiness.markProducerTopicsDown(mismatches, "生产 Topic 配置漂移");
            log.error("VMD 生产 Topic 配置漂移（不自动变更）: {}", mismatches);
        }
    }

    private void validateOne(VmdKafkaTopicSpec spec,
                             TopicDescription description,
                             Map<ConfigResource, Config> configs,
                             List<VmdKafkaTopicReadiness.TopicConfigMismatch> mismatches) {
        // 分区数小于期望 → 上报
        int actualPartitions = description.partitions().size();
        if (actualPartitions < spec.partitions()) {
            mismatches.add(new VmdKafkaTopicReadiness.TopicConfigMismatch(
                    spec.topicName(), "PARTITIONS",
                    String.valueOf(spec.partitions()), String.valueOf(actualPartitions)));
            recordConfigMismatch(spec.topicName(), "PARTITIONS");
        }
        // 副本数不一致 → 上报
        short actualReplication = replicationFactorOf(description);
        if (actualReplication != spec.replicationFactor()) {
            mismatches.add(new VmdKafkaTopicReadiness.TopicConfigMismatch(
                    spec.topicName(), "REPLICATION_FACTOR",
                    String.valueOf(spec.replicationFactor()), String.valueOf(actualReplication)));
            recordConfigMismatch(spec.topicName(), "REPLICATION_FACTOR");
        }
        // 关键配置（cleanup.policy）不一致 → 上报
        for (Map.Entry<String, String> expected : spec.configs().entrySet()) {
            String actual = configValue(configs, spec.topicName(), expected.getKey());
            if (actual == null || !expected.getValue().equals(actual)) {
                mismatches.add(new VmdKafkaTopicReadiness.TopicConfigMismatch(
                        spec.topicName(), "CONFIG_" + expected.getKey(),
                        expected.getValue(), actual));
                recordConfigMismatch(spec.topicName(), "CONFIG_" + expected.getKey());
            }
        }
        if (hasMismatch(spec.topicName(), mismatches)) {
            recordInit(spec.topicName(), "mismatch");
        } else {
            recordInit(spec.topicName(), "ok");
        }
    }

    private boolean hasMismatch(String topic, List<VmdKafkaTopicReadiness.TopicConfigMismatch> mismatches) {
        return mismatches.stream().anyMatch(m -> topic.equals(m.topic()));
    }

    /**
     * 观测消费 Topic 可访问性探测（不创建）。
     */
    private void probeInventoryObserved() {
        String topic = topicProperties.topic(VmdKafkaLogicalTopic.INVENTORY_OBSERVED);
        try {
            describe(List.of(topic));
            readiness.markInventoryObservedUp();
            recordInit(topic, "present");
            log.info("VMD 观测 Topic 可访问: topic={}", topic);
        } catch (UnknownTopicOrPartitionException e) {
            readiness.markInventoryObservedNotPresent();
            recordInit(topic, "absent");
            log.warn("VMD 观测 Topic 暂不存在（上游未就绪，VMD 不创建）: topic={}", topic);
        } catch (Exception e) {
            readiness.markInventoryObservedDown("观测 Topic 探测失败: " + e.getMessage());
            recordInit(topic, "error");
            log.error("VMD 观测 Topic 探测失败: topic={}, error={}", topic, e.getMessage(), e);
        }
    }

    private Map<String, TopicDescription> describe(List<String> names) throws Exception {
        DescribeTopicsResult result = admin.describeTopics(names);
        Map<String, TopicDescription> descriptions = new HashMap<>();
        for (Map.Entry<String, KafkaFuture<TopicDescription>> entry : result.values().entrySet()) {
            try {
                descriptions.put(entry.getKey(),
                        entry.getValue().get(DESCRIBE_TIMEOUT_MS, TimeUnit.MILLISECONDS));
            } catch (ExecutionException | TimeoutException e) {
                throw unwrap(e);
            }
        }
        return descriptions;
    }

    private Map<ConfigResource, Config> describeConfigs(List<VmdKafkaTopicSpec> specs) throws Exception {
        List<ConfigResource> resources = specs.stream()
                .map(spec -> new ConfigResource(ConfigResource.Type.TOPIC, spec.topicName()))
                .toList();
        DescribeConfigsResult result = admin.describeConfigs(resources);
        try {
            return result.all().get(DESCRIBE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (ExecutionException | TimeoutException e) {
            throw unwrap(e);
        }
    }

    private short replicationFactorOf(TopicDescription description) {
        if (description.partitions() == null || description.partitions().isEmpty()) {
            return 0;
        }
        TopicPartitionInfo first = description.partitions().get(0);
        return first.replicas() == null ? 0 : (short) first.replicas().size();
    }

    private String configValue(Map<ConfigResource, Config> configs, String topic, String key) {
        Config config = configs.get(new ConfigResource(ConfigResource.Type.TOPIC, topic));
        if (config == null) {
            return null;
        }
        ConfigEntry entry = config.get(key);
        return entry == null ? null : entry.value();
    }

    private VmdKafkaTopicMetrics metrics() {
        return metricsProvider == null ? null : metricsProvider.getIfAvailable();
    }

    private void recordInit(String topic, String result) {
        VmdKafkaTopicMetrics metrics = metrics();
        if (metrics != null) {
            metrics.recordInit(topic, result);
        }
    }

    private void recordConfigMismatch(String topic, String type) {
        VmdKafkaTopicMetrics metrics = metrics();
        if (metrics != null) {
            metrics.recordConfigMismatch(topic, type);
        }
    }

    private Exception unwrap(Throwable ex) {
        Throwable t = ex;
        while ((t instanceof ExecutionException) && t.getCause() != null) {
            t = t.getCause();
        }
        if (t instanceof Exception e) {
            return e;
        }
        return new IllegalStateException("Kafka Admin 调用失败", t);
    }
}
