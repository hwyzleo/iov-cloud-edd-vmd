package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MdmSyncMetrics单元测试
 */
class MdmSyncMetricsTest {

    private MeterRegistry meterRegistry;
    private MdmSyncMetrics metrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metrics = new MdmSyncMetrics(meterRegistry);
    }

    @Test
    void testRecordSuccess() {
        // Given
        metrics.recordFailure(); // 先记录一次失败
        assertEquals(1, metrics.getConsecutiveFailures());

        // When
        metrics.recordSuccess();

        // Then
        assertEquals(0, metrics.getConsecutiveFailures());
        assertTrue(metrics.getLastSyncTimestamp() > 0);
        
        Counter successCounter = meterRegistry.find("mdm.sync.part.success").counter();
        assertNotNull(successCounter);
        assertEquals(1.0, successCounter.count());
    }

    @Test
    void testRecordFailure() {
        // When
        metrics.recordFailure();
        metrics.recordFailure();
        metrics.recordFailure();

        // Then
        assertEquals(3, metrics.getConsecutiveFailures());
        
        Counter failureCounter = meterRegistry.find("mdm.sync.part.failure").counter();
        assertNotNull(failureCounter);
        assertEquals(3.0, failureCounter.count());
    }

    @Test
    void testRecordDuration() {
        // When
        metrics.recordDuration(1000);
        metrics.recordDuration(2000);

        // Then
        Timer timer = meterRegistry.find("mdm.sync.part.duration").timer();
        assertNotNull(timer);
        assertEquals(2, timer.count());
        assertEquals(3000, timer.totalTime(TimeUnit.MILLISECONDS));
    }

    @Test
    void testGetSyncDelayMinutes_NeverSynced() {
        // When
        long delay = metrics.getSyncDelayMinutes();

        // Then
        assertEquals(-1, delay);
    }

    @Test
    void testGetSyncDelayMinutes_RecentlySynced() {
        // Given
        metrics.recordSuccess();

        // When
        long delay = metrics.getSyncDelayMinutes();

        // Then
        assertEquals(0, delay);
    }

    @Test
    void testResetConsecutiveFailures() {
        // Given
        metrics.recordFailure();
        metrics.recordFailure();
        assertEquals(2, metrics.getConsecutiveFailures());

        // When
        metrics.resetConsecutiveFailures();

        // Then
        assertEquals(0, metrics.getConsecutiveFailures());
    }
}
