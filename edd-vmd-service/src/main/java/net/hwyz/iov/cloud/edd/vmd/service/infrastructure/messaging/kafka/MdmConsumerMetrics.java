package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MDM 消费 Topic 可观测性指标（VMD-DSN-CR-052 §8.2）
 * <ul>
 *   <li>{@code vmd_mdm_topic_check_total{topic,result}}：启动预检结果（present / missing / unauthorized / timeout / error）</li>
 *   <li>{@code vmd_mdm_consumer_lag{projection,topic}}：消费 Lag（-1 表示未知）</li>
 *   <li>{@code vmd_mdm_consume_total{projection,result}}：消费结果（success / failure / parse_error）</li>
 *   <li>{@code vmd_mdm_event_ignored_total{projection,reason}}：版本门禁忽略（duplicate / stale）</li>
 *   <li>{@code vmd_mdm_projection_bootstrap{projection,status}}：Bootstrap 基线状态（completed / failed / skipped）</li>
 *   <li>{@code vmd_mdm_projection_version_gap{projection}}：本地投影与上游版本差（Gauge）</li>
 * </ul>
 *
 * @author hwyz_leo
 */
@Component
@RequiredArgsConstructor
public class MdmConsumerMetrics {

    private final MeterRegistry meterRegistry;
    private final ConcurrentMap<String, AtomicLong> lagGauges = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicLong> versionGapGauges = new ConcurrentHashMap<>();

    /**
     * 记录单 Topic 预检结果。
     *
     * @param topic  实际 Topic 名称
     * @param result present / missing / unauthorized / timeout / error
     */
    public void recordTopicCheck(String topic, String result) {
        meterRegistry.counter("vmd_mdm_topic_check_total", "topic", topic, "result", result).increment();
    }

    /**
     * 记录消费 Lag（Gauge，按 projection 幂等注册）。
     *
     * @param projection 投影类型
     * @param topic      实际 Topic
     * @param lag        Lag（-1 表示未知）
     */
    public void recordLag(MdmProjectionType projection, String topic, long lag) {
        AtomicLong value = lagGauges.computeIfAbsent(projection.configKey(), k -> {
            AtomicLong v = new AtomicLong(-1);
            meterRegistry.gauge("vmd_mdm_consumer_lag",
                    Tags.of("projection", projection.configKey(), "topic", topic), v);
            return v;
        });
        value.set(lag);
    }

    /**
     * 记录消费结果。
     *
     * @param projection 投影类型
     * @param result     success / failure / parse_error
     */
    public void recordConsume(MdmProjectionType projection, String result) {
        meterRegistry.counter("vmd_mdm_consume_total",
                "projection", projection.configKey(), "result", result).increment();
    }

    /**
     * 记录版本门禁忽略事件。
     *
     * @param projection 投影类型
     * @param reason     duplicate（重复事件）/ stale（低版本事件）
     */
    public void recordIgnored(MdmProjectionType projection, String reason) {
        meterRegistry.counter("vmd_mdm_event_ignored_total",
                "projection", projection.configKey(), "reason", reason).increment();
    }

    /**
     * 记录 Bootstrap 基线结果。
     *
     * @param projection 投影类型
     * @param status     completed / failed / skipped
     */
    public void recordBootstrap(MdmProjectionType projection, String status) {
        meterRegistry.counter("vmd_mdm_projection_bootstrap",
                "projection", projection.configKey(), "status", status).increment();
    }

    /**
     * 记录本地投影与上游版本差（Gauge，按 projection 幂等注册）。
     *
     * @param projection 投影类型
     * @param gap        版本差（>=0；-1 表示未知）
     */
    public void recordVersionGap(MdmProjectionType projection, long gap) {
        AtomicLong value = versionGapGauges.computeIfAbsent(projection.configKey(), k -> {
            AtomicLong v = new AtomicLong(-1);
            meterRegistry.gauge("vmd_mdm_projection_version_gap",
                    Tags.of("projection", projection.configKey()), v);
            return v;
        });
        value.set(gap);
    }
}
