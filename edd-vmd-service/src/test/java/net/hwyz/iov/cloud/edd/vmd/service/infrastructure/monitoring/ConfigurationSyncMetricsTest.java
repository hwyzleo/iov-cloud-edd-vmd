package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConfigurationSyncMetrics单元测试（CR-047 §8）
 */
class ConfigurationSyncMetricsTest {

    private MeterRegistry meterRegistry;
    private ConfigurationSyncMetrics metrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metrics = new ConfigurationSyncMetrics(meterRegistry);
    }

    @Test
    @DisplayName("应记录Bootstrap总数/成功/失败计数")
    void testRecordBootstrap() {
        // When
        metrics.recordBootstrapTotal();
        metrics.recordBootstrapSuccess();
        metrics.recordBootstrapTotal();
        metrics.recordBootstrapFailure();

        // Then
        Counter totalCounter = meterRegistry.find("vmd.mdm.configuration.bootstrap.total").counter();
        Counter successCounter = meterRegistry.find("vmd.mdm.configuration.bootstrap.success").counter();
        Counter failureCounter = meterRegistry.find("vmd.mdm.configuration.bootstrap.failure").counter();
        assertNotNull(totalCounter);
        assertNotNull(successCounter);
        assertNotNull(failureCounter);
        assertEquals(2.0, totalCounter.count());
        assertEquals(1.0, successCounter.count());
        assertEquals(1.0, failureCounter.count());
    }

    @Test
    @DisplayName("应记录Kafka旧版本/失败/删除事件计数")
    void testRecordKafkaEvents() {
        // When
        metrics.recordStaleEvent();
        metrics.recordFailure();
        metrics.recordDeletedEvent();

        // Then
        Counter staleCounter = meterRegistry.find("vmd.mdm.configuration.kafka.stale").counter();
        Counter failedCounter = meterRegistry.find("vmd.mdm.configuration.kafka.failed").counter();
        Counter deletedCounter = meterRegistry.find("vmd.mdm.configuration.kafka.deleted").counter();
        assertEquals(1.0, staleCounter.count());
        assertEquals(1.0, failedCounter.count());
        assertEquals(1.0, deletedCounter.count());
    }

    @Test
    @DisplayName("应记录选项反查零匹配/多匹配计数")
    void testRecordOptionReverse() {
        // When
        metrics.recordOptionReverseNoMatch();
        metrics.recordOptionReverseMultipleMatch();

        // Then
        Counter noMatchCounter = meterRegistry.find("vmd.mdm.configuration.option_reverse.no_match").counter();
        Counter multipleMatchCounter = meterRegistry.find("vmd.mdm.configuration.option_reverse.multiple_match").counter();
        assertEquals(1.0, noMatchCounter.count());
        assertEquals(1.0, multipleMatchCounter.count());
    }

    @Test
    @DisplayName("应记录缺失Variant引用与层级缺失巡检指标")
    void testRecordIntegrityCounters() {
        // When
        metrics.recordMissingVariant(3);
        metrics.recordHierarchyMismatch(5);

        // Then
        assertEquals(3, metrics.getMissingVariantCount());
        assertEquals(5, metrics.getHierarchyMismatchCount());
    }
}
