package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * MDM 消费 Topic 可观测性指标单元测试（VMD-DSN-CR-052 §8.2）
 * <p>
 * 验证六类指标按 projection/topic 标签正确记录与幂等注册（Gauge 不重复注册）。
 *
 * @author hwyz_leo
 */
@DisplayName("MdmConsumerMetrics 测试")
class MdmConsumerMetricsTest {

    private MeterRegistry registry;
    private MdmConsumerMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new MdmConsumerMetrics(registry);
    }

    @Test
    @DisplayName("预检结果指标 vmd_mdm_topic_check_total")
    void topicCheckCounter() {
        metrics.recordTopicCheck("mdm.brand", "present");
        metrics.recordTopicCheck("mdm.brand", "present");
        metrics.recordTopicCheck("mdm.part", "missing");
        assertEquals(2.0, registry.counter("vmd_mdm_topic_check_total",
                "topic", "mdm.brand", "result", "present").count());
        assertEquals(1.0, registry.counter("vmd_mdm_topic_check_total",
                "topic", "mdm.part", "result", "missing").count());
    }

    @Test
    @DisplayName("消费结果指标 vmd_mdm_consume_total 按投影记录")
    void consumeCounter() {
        metrics.recordConsume(MdmProjectionType.BRAND, "success");
        metrics.recordConsume(MdmProjectionType.BRAND, "failure");
        metrics.recordConsume(MdmProjectionType.BRAND, "failure");
        assertEquals(1.0, registry.counter("vmd_mdm_consume_total",
                "projection", "brand", "result", "success").count());
        assertEquals(2.0, registry.counter("vmd_mdm_consume_total",
                "projection", "brand", "result", "failure").count());
    }

    @Test
    @DisplayName("版本门禁忽略指标 vmd_mdm_event_ignored_total 按原因记录")
    void ignoredCounter() {
        metrics.recordIgnored(MdmProjectionType.MODEL, "duplicate");
        metrics.recordIgnored(MdmProjectionType.MODEL, "stale");
        metrics.recordIgnored(MdmProjectionType.MODEL, "stale");
        assertEquals(1.0, registry.counter("vmd_mdm_event_ignored_total",
                "projection", "model", "reason", "duplicate").count());
        assertEquals(2.0, registry.counter("vmd_mdm_event_ignored_total",
                "projection", "model", "reason", "stale").count());
    }

    @Test
    @DisplayName("Bootstrap 指标 vmd_mdm_projection_bootstrap 按状态记录")
    void bootstrapCounter() {
        metrics.recordBootstrap(MdmProjectionType.PLANT, "completed");
        metrics.recordBootstrap(MdmProjectionType.PLANT, "skipped");
        assertEquals(1.0, registry.counter("vmd_mdm_projection_bootstrap",
                "projection", "plant", "status", "completed").count());
        assertEquals(1.0, registry.counter("vmd_mdm_projection_bootstrap",
                "projection", "plant", "status", "skipped").count());
    }

    @Test
    @DisplayName("Lag / 版本差 Gauge 幂等注册并更新")
    void lagAndVersionGapGauge() {
        metrics.recordLag(MdmProjectionType.BRAND, "mdm.brand", 10);
        metrics.recordLag(MdmProjectionType.BRAND, "mdm.brand", 7);
        metrics.recordVersionGap(MdmProjectionType.BRAND, 3);
        metrics.recordVersionGap(MdmProjectionType.BRAND, 1);

        assertEquals(7.0, registry.get("vmd_mdm_consumer_lag")
                .tags("projection", "brand", "topic", "mdm.brand").gauge().value(), 0.001);
        assertEquals(1.0, registry.get("vmd_mdm_projection_version_gap")
                .tags("projection", "brand").gauge().value(), 0.001);
        // 重复 record 不重复注册（幂等）
        assertEquals(1, registry.getMeters().stream()
                .filter(m -> "vmd_mdm_consumer_lag".equals(m.getId().getName())).count());
    }
}
