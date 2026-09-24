package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * VMD Kafka Topic 初始化可观测性指标（VMD-DSN-CR-051）
 * <ul>
 *   <li>{@code vmd_kafka_topic_init_total{topic,result}}：初始化/校验结果（ok / mismatch / down / absent / error）</li>
 *   <li>{@code vmd_kafka_topic_config_mismatch{topic,type}}：配置漂移计数（PARTITIONS / REPLICATION_FACTOR / CONFIG）</li>
 * </ul>
 *
 * @author hwyz_leo
 */
@Component
@RequiredArgsConstructor
@ConditionalOnBean(MeterRegistry.class)
public class VmdKafkaTopicMetrics {

    private final MeterRegistry meterRegistry;

    /**
     * 记录单 Topic 初始化/校验结果。
     *
     * @param topic  实际 Topic 名称
     * @param result 结果（ok / mismatch / down / absent / error）
     */
    public void recordInit(String topic, String result) {
        meterRegistry.counter("vmd_kafka_topic_init_total", "topic", topic, "result", result).increment();
    }

    /**
     * 记录 Topic 配置漂移。
     *
     * @param topic 实际 Topic 名称
     * @param type  漂移类型（PARTITIONS / REPLICATION_FACTOR / CONFIG）
     */
    public void recordConfigMismatch(String topic, String type) {
        meterRegistry.counter("vmd_kafka_topic_config_mismatch", "topic", topic, "type", type).increment();
    }
}
