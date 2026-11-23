package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 车辆首次激活事实消费指标（VMD-DSN-CR-077 §8.1，R-18 #9 Micrometer）
 * <ul>
 *   <li>{@code vmd_activation_fact_consume_total{topic,result}}：
 *       {@code success / duplicate / corrected / quarantined / failed}
 *       （{@code conflict} 按 R-10 取消）</li>
 *   <li>{@code vmd_activation_fact_digest_mismatch_total{topic}}：摘要差异（观测，不改变终态）。
 *       同时覆盖两类：① 业务键重投时台账摘要不同；② 事件摘要与本地复算不一致
 *       （{@code DIGEST_VERIFY_MISMATCH}，事件摘要仍为权威值落库，不降级 LOCAL）</li>
 *   <li>{@code vmd_activation_fact_vin_unresolved_total{topic}}：未建档 VIN</li>
 *   <li>{@code vmd_activation_fact_digest_source_total{source}}：EVENT（事件提供，权威）/ LOCAL（本地复算）</li>
 * </ul>
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@ConditionalOnClass(MeterRegistry.class)
public class VmdActivationFactMetrics {

    private static final String CONSUME_TOTAL = "vmd_activation_fact_consume_total";
    private static final String DIGEST_MISMATCH_TOTAL = "vmd_activation_fact_digest_mismatch_total";
    private static final String VIN_UNRESOLVED_TOTAL = "vmd_activation_fact_vin_unresolved_total";
    private static final String DIGEST_SOURCE_TOTAL = "vmd_activation_fact_digest_source_total";

    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    public VmdActivationFactMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * 记录消费结果
     *
     * @param topic  Topic
     * @param result success / duplicate / corrected / quarantined / failed
     */
    public void recordConsume(String topic, String result) {
        counter(CONSUME_TOTAL, "topic", topic, "result", result).increment();
    }

    /**
     * 记录摘要差异（观测项，不改变终态）
     * <p>
     * 两类来源：① 业务键重投时台账摘要不同；② 事件摘要与本地复算不一致（完整性校验）。
     *
     * @param topic Topic
     */
    public void recordDigestMismatch(String topic) {
        counter(DIGEST_MISMATCH_TOTAL, "topic", topic, null, null).increment();
    }

    /**
     * 记录 VIN 未建档
     *
     * @param topic Topic
     */
    public void recordVinUnresolved(String topic) {
        counter(VIN_UNRESOLVED_TOTAL, "topic", topic, null, null).increment();
    }

    /**
     * 记录摘要来源
     *
     * @param source EVENT / LOCAL
     */
    public void recordDigestSource(String source) {
        counter(DIGEST_SOURCE_TOTAL, "source", source, null, null).increment();
    }

    private Counter counter(String name, String tagKey, String tagValue, String secondKey, String secondValue) {
        String cacheKey = name + '|' + tagKey + '=' + tagValue + (secondKey == null ? "" : '|' + secondKey + '=' + secondValue);
        return counters.computeIfAbsent(cacheKey, key -> {
            io.micrometer.core.instrument.Counter.Builder builder =
                    io.micrometer.core.instrument.Counter.builder(name);
            if (tagKey != null) {
                builder.tag(tagKey, String.valueOf(tagValue));
            }
            if (secondKey != null) {
                builder.tag(secondKey, String.valueOf(secondValue));
            }
            return builder.register(meterRegistry);
        });
    }

}
