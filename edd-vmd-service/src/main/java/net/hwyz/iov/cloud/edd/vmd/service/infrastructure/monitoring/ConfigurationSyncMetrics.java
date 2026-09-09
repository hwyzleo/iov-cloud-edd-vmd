package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Configuration 投影同步监控指标（CR-047 §8）
 * <p>
 * 记录 Configuration 最小投影同步链路关键指标：
 * - Bootstrap 总数 / 成功 / 失败
 * - 投影完整性：缺失 Variant 引用、产品树层级缺失（迁移期巡检指标）
 * - Kafka 事件：旧版本（stale）/ 失败 / 删除
 * - 查询：getConfiguration 耗时、选项反查零匹配 / 多匹配
 * </p>
 *
 * @author CR-047
 */
@Slf4j
@Component
public class ConfigurationSyncMetrics {

    private final Counter bootstrapTotalCounter;
    private final Counter bootstrapSuccessCounter;
    private final Counter bootstrapFailureCounter;
    private final Counter staleEventCounter;
    private final Counter failedEventCounter;
    private final Counter deletedEventCounter;
    private final Counter optionReverseNoMatchCounter;
    private final Counter optionReverseMultipleMatchCounter;
    private final Timer getConfigurationTimer;
    private final AtomicLong missingVariantCount;
    private final AtomicLong hierarchyMismatchCount;

    public ConfigurationSyncMetrics(MeterRegistry meterRegistry) {
        this.bootstrapTotalCounter = Counter.builder("vmd.mdm.configuration.bootstrap.total")
                .description("Configuration Bootstrap 同步总次数")
                .register(meterRegistry);
        this.bootstrapSuccessCounter = Counter.builder("vmd.mdm.configuration.bootstrap.success")
                .description("Configuration Bootstrap 同步成功次数")
                .register(meterRegistry);
        this.bootstrapFailureCounter = Counter.builder("vmd.mdm.configuration.bootstrap.failure")
                .description("Configuration Bootstrap 同步失败次数")
                .register(meterRegistry);
        this.staleEventCounter = Counter.builder("vmd.mdm.configuration.kafka.stale")
                .description("Configuration Kafka 旧版本/重复事件忽略次数")
                .register(meterRegistry);
        this.failedEventCounter = Counter.builder("vmd.mdm.configuration.kafka.failed")
                .description("Configuration Kafka 事件处理失败次数")
                .register(meterRegistry);
        this.deletedEventCounter = Counter.builder("vmd.mdm.configuration.kafka.deleted")
                .description("Configuration Kafka 删除/失效事件处理次数")
                .register(meterRegistry);
        this.optionReverseNoMatchCounter = Counter.builder("vmd.mdm.configuration.option_reverse.no_match")
                .description("选项反查零匹配次数")
                .register(meterRegistry);
        this.optionReverseMultipleMatchCounter = Counter.builder("vmd.mdm.configuration.option_reverse.multiple_match")
                .description("选项反查多匹配次数")
                .register(meterRegistry);
        this.getConfigurationTimer = Timer.builder("vmd.mdm.configuration.query.duration")
                .description("getConfiguration 查询耗时")
                .register(meterRegistry);

        this.missingVariantCount = new AtomicLong(0);
        Gauge.builder("vmd.mdm.configuration.projection.missing_variant", missingVariantCount, AtomicLong::get)
                .description("配置投影缺失 Variant 引用条数")
                .register(meterRegistry);
        this.hierarchyMismatchCount = new AtomicLong(0);
        Gauge.builder("vmd.mdm.configuration.hierarchy_mismatch", hierarchyMismatchCount, AtomicLong::get)
                .description("配置投影产品树层级缺失条数（迁移期巡检，完成后降为巡检指标）")
                .register(meterRegistry);
    }

    /**
     * 记录 Bootstrap 处理总数
     */
    public void recordBootstrapTotal() {
        bootstrapTotalCounter.increment();
    }

    /**
     * 记录 Bootstrap 成功
     */
    public void recordBootstrapSuccess() {
        bootstrapSuccessCounter.increment();
    }

    /**
     * 记录 Bootstrap 失败
     */
    public void recordBootstrapFailure() {
        bootstrapFailureCounter.increment();
    }

    /**
     * 记录旧版本/重复事件忽略
     */
    public void recordStaleEvent() {
        staleEventCounter.increment();
    }

    /**
     * 记录事件处理失败
     */
    public void recordFailure() {
        failedEventCounter.increment();
    }

    /**
     * 记录删除/失效事件处理
     */
    public void recordDeletedEvent() {
        deletedEventCounter.increment();
    }

    /**
     * 记录选项反查零匹配
     */
    public void recordOptionReverseNoMatch() {
        optionReverseNoMatchCounter.increment();
    }

    /**
     * 记录选项反查多匹配
     */
    public void recordOptionReverseMultipleMatch() {
        optionReverseMultipleMatchCounter.increment();
    }

    /**
     * 记录 getConfiguration 查询耗时
     *
     * @param durationMs 耗时（毫秒）
     */
    public void recordGetConfigurationDuration(long durationMs) {
        getConfigurationTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 记录缺失 Variant 引用条数（ProjectionIntegrityChecker）
     *
     * @param count 缺失条数
     */
    public void recordMissingVariant(long count) {
        missingVariantCount.set(count);
    }

    /**
     * 记录产品树层级缺失条数（ProjectionIntegrityChecker）
     *
     * @param count 缺失条数
     */
    public void recordHierarchyMismatch(long count) {
        hierarchyMismatchCount.set(count);
    }

    /**
     * 获取缺失 Variant 引用条数
     *
     * @return 条数
     */
    public long getMissingVariantCount() {
        return missingVariantCount.get();
    }

    /**
     * 获取产品树层级缺失条数
     *
     * @return 条数
     */
    public long getHierarchyMismatchCount() {
        return hierarchyMismatchCount.get();
    }
}
