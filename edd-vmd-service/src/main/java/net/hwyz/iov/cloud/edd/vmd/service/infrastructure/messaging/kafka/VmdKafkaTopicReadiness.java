package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * VMD Kafka Topic Readiness 状态（VMD-DSN-CR-051）
 * <p>
 * 生产 Topic（PRODUCER_OWNED）与观测消费 Topic（CONSUMER_ONLY）独立维护：
 * 生产 Topic 配置漂移 / 初始化失败时置 DOWN，Producer 业务流量受其门禁；
 * 观测 Topic 仅做可访问性探测，短时不存在不置 DOWN（上游未就绪，非 VMD 故障）。
 *
 * @author hwyz_leo
 */
@Component
public class VmdKafkaTopicReadiness {

    /**
     * 就绪状态。
     */
    public enum State {
        /**
         * 尚未完成初始化评估。
         */
        INIT,
        /**
         * 就绪。
         */
        UP,
        /**
         * 不可用 / 配置不兼容。
         */
        DOWN,
        /**
         * 观测 Topic 当前不存在（上游未就绪），仅消费侧信息项。
         */
        NOT_PRESENT
    }

    /**
     * Topic 配置漂移明细。
     *
     * @param topic    实际 Topic 名称
     * @param type     漂移类型（PARTITIONS / REPLICATION_FACTOR / CONFIG）
     * @param expected 期望值
     * @param actual   实际值
     */
    public record TopicConfigMismatch(String topic, String type, String expected, String actual) {
    }

    private final AtomicReference<State> producerTopics = new AtomicReference<>(State.INIT);
    private final AtomicReference<List<TopicConfigMismatch>> mismatches = new AtomicReference<>(List.of());
    private final AtomicReference<String> producerReason = new AtomicReference<>();
    private final AtomicReference<State> inventoryObserved = new AtomicReference<>(State.INIT);
    private final AtomicReference<String> inventoryObservedReason = new AtomicReference<>();

    /**
     * 生产 Topic 就绪状态。
     */
    public State producerTopicsState() {
        return producerTopics.get();
    }

    /**
     * 生产 Topic 配置漂移明细（不可变）。
     */
    public List<TopicConfigMismatch> producerMismatches() {
        return mismatches.get();
    }

    /**
     * 生产 Topic 不可用原因。
     */
    public String producerReason() {
        return producerReason.get();
    }

    /**
     * 生产 Topic 全部校验通过。
     */
    public void markProducerTopicsUp() {
        producerTopics.set(State.UP);
        mismatches.set(List.of());
        producerReason.set(null);
    }

    /**
     * 生产 Topic 存在配置漂移或初始化失败。
     *
     * @param mismatches 漂移明细（可空）
     * @param reason     失败原因
     */
    public void markProducerTopicsDown(List<TopicConfigMismatch> mismatches, String reason) {
        producerTopics.set(State.DOWN);
        this.mismatches.set(mismatches == null ? List.of() : List.copyOf(mismatches));
        producerReason.set(reason);
    }

    /**
     * 观测消费 Topic 就绪状态。
     */
    public State inventoryObservedState() {
        return inventoryObserved.get();
    }

    /**
     * 观测消费 Topic 状态原因。
     */
    public String inventoryObservedReason() {
        return inventoryObservedReason.get();
    }

    /**
     * 观测消费 Topic 可访问（已存在且 ACL 通过）。
     */
    public void markInventoryObservedUp() {
        inventoryObserved.set(State.UP);
        inventoryObservedReason.set(null);
    }

    /**
     * 观测消费 Topic 当前不存在（上游未就绪）。
     */
    public void markInventoryObservedNotPresent() {
        inventoryObserved.set(State.NOT_PRESENT);
        inventoryObservedReason.set("上游 Topic 暂不存在（IOV-OTA 未创建），VMD 不创建");
    }

    /**
     * 观测消费 Topic 探测失败（ACL 拒绝 / Broker 异常）。
     *
     * @param reason 失败原因
     */
    public void markInventoryObservedDown(String reason) {
        inventoryObserved.set(State.DOWN);
        inventoryObservedReason.set(reason);
    }
}
