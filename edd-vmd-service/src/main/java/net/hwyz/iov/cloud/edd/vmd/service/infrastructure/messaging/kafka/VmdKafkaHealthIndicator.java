package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicProvisioningStatus;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * VMD Kafka 健康检查（VMD-DSN-CR-051）
 * <p>
 * 生产 Topic（producerTopics）与观测消费 Topic（inventoryObservedConsumer）分离，
 * 便于区分 VMD 自有 Topic 故障与上游 Topic 未就绪：
 * <ul>
 *   <li>producerTopics：FW-KAFKA Provisioning 状态（Broker 不可用 / 无权限 / 缺失 → NOT_READY）
 *       或 VMD 漂移校验 DOWN 时报告 DOWN</li>
 *   <li>inventoryObservedConsumer：观测 Topic 可访问性；仅消费者启用时上报（默认暗部署关闭）</li>
 * </ul>
 *
 * @author hwyz_leo
 */
@Component
@RequiredArgsConstructor
@ConditionalOnClass(HealthIndicator.class)
public class VmdKafkaHealthIndicator implements HealthIndicator {

    private final ObjectProvider<KafkaTopicProvisioningStatus> provisioningStatusProvider;
    private final VmdKafkaTopicReadiness readiness;

    /**
     * 观测消费者是否启用（默认关闭）。
     */
    @Value("${vmd.software-inventory.observed.kafka.enabled:false}")
    private boolean observedEnabled;

    @Override
    public Health health() {
        boolean down = false;
        Health.Builder builder = new Health.Builder();

        // 生产 Topic 健康项
        down |= buildProducerTopics(builder);

        // 观测消费 Topic 健康项（独立，仅启用时上报）
        if (observedEnabled) {
            down |= buildInventoryObserved(builder);
        }

        builder.status(down ? Status.DOWN : Status.UP);
        return builder.build();
    }

    private boolean buildProducerTopics(Health.Builder builder) {
        KafkaTopicProvisioningStatus provisioning = provisioningStatusProvider.getIfAvailable();

        // 框架 Provisioning 未装配（DISABLED）→ 无托管 Topic，视为健康
        if (provisioning == null || provisioning.state() == KafkaTopicProvisioningStatus.State.DISABLED) {
            builder.withDetail("producerTopics", "UP")
                    .withDetail("producerTopics.state", "DISABLED")
                    .withDetail("producerTopics.note", "Topic Provisioning 未启用，VMD 不托管生产 Topic");
            return false;
        }

        // Broker 不可用 / 无权限 / 缺失 → 框架状态 NOT_READY
        if (provisioning.state() == KafkaTopicProvisioningStatus.State.NOT_READY) {
            builder.withDetail("producerTopics", "DOWN")
                    .withDetail("producerTopics.state", "NOT_READY")
                    .withDetail("producerTopics.missingTopics", String.join(",", provisioning.missingTopics()))
                    .withDetail("producerTopics.lastFailure",
                            provisioning.lastFailure().map(Throwable::getMessage).orElse("unknown"));
            return true;
        }

        // VMD 漂移校验
        if (readiness.producerTopicsState() == VmdKafkaTopicReadiness.State.DOWN) {
            String mismatches = readiness.producerMismatches().stream()
                    .map(m -> m.topic() + ":" + m.type() + "(expected=" + m.expected() + ",actual=" + m.actual() + ")")
                    .collect(Collectors.joining(";"));
            builder.withDetail("producerTopics", "DOWN")
                    .withDetail("producerTopics.state", "DOWN")
                    .withDetail("producerTopics.reason", readiness.producerReason())
                    .withDetail("producerTopics.mismatches", mismatches);
            return true;
        }

        builder.withDetail("producerTopics", "UP")
                .withDetail("producerTopics.state", "READY")
                .withDetail("producerTopics.mismatches", "none");
        return false;
    }

    private boolean buildInventoryObserved(Health.Builder builder) {
        VmdKafkaTopicReadiness.State state = readiness.inventoryObservedState();
        builder.withDetail("inventoryObservedConsumer", state.name());
        switch (state) {
            case DOWN -> {
                builder.withDetail("inventoryObservedConsumer.reason", readiness.inventoryObservedReason());
                return true;
            }
            case NOT_PRESENT -> {
                builder.withDetail("inventoryObservedConsumer.reason", readiness.inventoryObservedReason());
                return false;
            }
            case INIT, UP -> {
                return false;
            }
            default -> {
                return false;
            }
        }
    }
}
