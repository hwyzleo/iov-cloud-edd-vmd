package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.VmdActivationFactConsumerProperties;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.VmdKafkaTopicProperties;
import net.hwyz.iov.cloud.framework.kafka.properties.TopicProvisioningProperties;
import net.hwyz.iov.cloud.framework.kafka.support.KafkaAdminOperations;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.errors.TopicAuthorizationException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * 车辆首次激活事实消费 Topic 预检探针（VMD-DSN-CR-077 §8.2 / D-VMD-077-02）
 * <p>
 * 只做「Topic 存在 + 可读」探测，<strong>绝不创建/改写</strong>上游 Topic
 * （{@code tsp.user-vehicle-activation-fact} 归 IOV-TSP 管理）。
 * <p>
 * E2E 环境 Topic 可能尚未创建：此时记为 {@link VmdActivationFactTopicReadiness.State#NOT_PRESENT}
 * 并<strong>不阻塞应用启动</strong>、不置健康 DOWN；后台按
 * {@code vmd.activation-fact.kafka.topic-probe-retry-interval-ms} 周期重探，就绪后发布事件
 * 触发启动协调器自恢复。探针本身不抛出异常（避免拖垮启动）。
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "vmd.activation-fact.kafka", name = "enabled", havingValue = "true")
public class VmdActivationFactTopicProbe {

    private static final Duration DEFAULT_DESCRIBE_TIMEOUT = Duration.ofSeconds(10);

    private final Admin admin;
    private final VmdKafkaTopicProperties topicProperties;
    private final VmdActivationFactConsumerProperties consumerProperties;
    private final VmdActivationFactTopicReadiness readiness;
    private final ApplicationEventPublisher eventPublisher;
    private final KafkaAdminOperations kafkaAdminOperations;
    private final ObjectProvider<TopicProvisioningProperties> frameworkTopicProvisioningProperties;

    /**
     * 应用就绪后执行首轮探测（失败不影响启动）。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        probe();
    }

    /**
     * 后台重探：未达 UP 时周期重试（就绪后自恢复）。
     */
    @Scheduled(fixedDelayString = "${vmd.activation-fact.kafka.topic-probe-retry-interval-ms:60000}")
    public void retryProbe() {
        if (readiness.state() == VmdActivationFactTopicReadiness.State.UP) {
            return;
        }
        probe();
    }

    /**
     * 执行一轮探测并发布结果事件。
     */
    public void probe() {
        String topic = topicProperties.topic(VmdKafkaLogicalTopic.USER_VEHICLE_ACTIVATION_FACT);
        int partitions = 0;
        try {
            Map<String, TopicDescription> descriptions = kafkaAdminOperations.describeTopics(
                    admin, List.of(topic), describeTimeout());
            TopicDescription description = descriptions.get(topic);
            partitions = description == null || description.partitions() == null
                    ? 0 : description.partitions().size();
            if (partitions < 1) {
                readiness.markNotPresent("Topic 无分区信息（上游未就绪）");
                log.warn("首次激活事实 Topic 无分区信息（上游未就绪，VMD 不创建）: topic={}", topic);
            } else {
                readiness.markUp(partitions);
                log.info("首次激活事实 Topic 可访问: topic={}, partitions={}", topic, partitions);
            }
        } catch (UnknownTopicOrPartitionException e) {
            readiness.markNotPresent("上游 Topic 暂不存在（IOV-TSP 未创建），VMD 不创建");
            log.warn("首次激活事实 Topic 暂不存在（不阻塞启动，后台重探以便就绪后自恢复）: topic={}", topic);
        } catch (TopicAuthorizationException e) {
            readiness.markDown("Topic Describe/Read 权限不足（不记录凭证）");
            log.error("首次激活事实 Topic 权限不足: topic={}", topic);
        } catch (TimeoutException | org.apache.kafka.common.errors.TimeoutException e) {
            readiness.markDown("Broker 超时或集群不可达");
            log.warn("首次激活事实 Topic 探测超时（瞬态，后台将重试）: topic={}", topic);
        } catch (Exception e) {
            // 探针永不抛出：任何异常只影响健康明细，不影响启动与消费
            readiness.markDown("探测异常: " + e.getClass().getSimpleName());
            log.warn("首次激活事实 Topic 探测异常（后台将重试）: topic={}, error={}", topic, e.getMessage());
        }
        eventPublisher.publishEvent(
                new VmdActivationFactTopicProbeEvent(readiness.state(), partitions));
    }

    /**
     * describe 等待预算：优先取 FW-KAFKA 配置，未启用 Provisioning 时回退默认 10s。
     *
     * @return 超时预算
     */
    private Duration describeTimeout() {
        TopicProvisioningProperties properties = frameworkTopicProvisioningProperties.getIfAvailable();
        return properties == null ? DEFAULT_DESCRIBE_TIMEOUT : properties.describeTimeout();
    }

    /**
     * 消费组 ID（日志/健康明细用）。
     *
     * @return 消费组
     */
    public String consumerGroup() {
        return consumerProperties.getGroupId();
    }

}
