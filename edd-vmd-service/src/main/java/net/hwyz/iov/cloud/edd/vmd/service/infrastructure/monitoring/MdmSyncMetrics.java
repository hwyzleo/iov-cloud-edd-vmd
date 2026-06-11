package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MDM同步监控指标
 * <p>
 * 记录MDM Part同步的关键指标，包括：
 * - 同步成功/失败次数
 * - 最后同步时间
 * - 同步延迟
 * - 同步耗时
 * </p>
 *
 * @author CR-024
 */
@Slf4j
@Component
public class MdmSyncMetrics {

    private final Counter syncSuccessCounter;
    private final Counter syncFailureCounter;
    private final Timer syncTimer;
    private final AtomicLong lastSyncTimestamp;
    private final AtomicInteger consecutiveFailures;
    private final MeterRegistry meterRegistry;

    public MdmSyncMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.lastSyncTimestamp = new AtomicLong(0);
        this.consecutiveFailures = new AtomicInteger(0);

        // 同步成功计数器
        this.syncSuccessCounter = Counter.builder("mdm.sync.part.success")
                .description("MDM Part同步成功次数")
                .register(meterRegistry);

        // 同步失败计数器
        this.syncFailureCounter = Counter.builder("mdm.sync.part.failure")
                .description("MDM Part同步失败次数")
                .register(meterRegistry);

        // 同步耗时计时器
        this.syncTimer = Timer.builder("mdm.sync.part.duration")
                .description("MDM Part同步耗时")
                .register(meterRegistry);

        // 最后同步时间
        Gauge.builder("mdm.sync.part.last.sync.timestamp", lastSyncTimestamp, AtomicLong::get)
                .description("MDM Part最后同步时间戳(毫秒)")
                .register(meterRegistry);

        // 连续失败次数
        Gauge.builder("mdm.sync.part.consecutive.failures", consecutiveFailures, AtomicInteger::get)
                .description("MDM Part同步连续失败次数")
                .register(meterRegistry);
    }

    /**
     * 记录同步成功
     */
    public void recordSuccess() {
        syncSuccessCounter.increment();
        lastSyncTimestamp.set(Instant.now().toEpochMilli());
        consecutiveFailures.set(0);
        log.debug("MDM Part同步成功，已重置连续失败计数");
    }

    /**
     * 记录同步失败
     */
    public void recordFailure() {
        syncFailureCounter.increment();
        int failures = consecutiveFailures.incrementAndGet();
        log.warn("MDM Part同步失败，连续失败次数: {}", failures);
    }

    /**
     * 记录同步耗时
     *
     * @param durationMs 耗时（毫秒）
     */
    public void recordDuration(long durationMs) {
        syncTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 获取最后同步时间戳
     *
     * @return 时间戳（毫秒），0表示从未同步
     */
    public long getLastSyncTimestamp() {
        return lastSyncTimestamp.get();
    }

    /**
     * 获取连续失败次数
     *
     * @return 连续失败次数
     */
    public int getConsecutiveFailures() {
        return consecutiveFailures.get();
    }

    /**
     * 计算同步延迟（分钟）
     *
     * @return 延迟分钟数，如果从未同步返回-1
     */
    public long getSyncDelayMinutes() {
        long lastSync = lastSyncTimestamp.get();
        if (lastSync == 0) {
            return -1;
        }
        return TimeUnit.MILLISECONDS.toMinutes(Instant.now().toEpochMilli() - lastSync);
    }

    /**
     * 重置连续失败计数
     */
    public void resetConsecutiveFailures() {
        consecutiveFailures.set(0);
    }
}
