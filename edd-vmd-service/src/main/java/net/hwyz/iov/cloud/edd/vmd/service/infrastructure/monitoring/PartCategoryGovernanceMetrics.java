package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 零件派生设备类别治理指标（EDD-VMD-DSN-CR-078 / D-VMD-78-08）
 * <p>
 * - vmd.part_category.inconsistency：派生类别与 MDM 节点类别不一致条数（巡检刷新）
 * - vmd.part_category.repair_total{trigger,result}：修复行为计数
 * - vmd.part_category.repair_failed_total{trigger}：修复整体异常计数（自动路径 best-effort）
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-11-25
 */
@Slf4j
@Component
public class PartCategoryGovernanceMetrics {

    private static final String GAUGE_INCONSISTENCY = "vmd.part_category.inconsistency";
    private static final String COUNTER_REPAIR = "vmd.part_category.repair_total";
    private static final String COUNTER_REPAIR_FAILED = "vmd.part_category.repair_failed_total";

    private final MeterRegistry meterRegistry;
    private final AtomicLong inconsistency = new AtomicLong(0L);
    private final Map<String, Counter> counterCache = new ConcurrentHashMap<>();

    public PartCategoryGovernanceMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        Gauge.builder(GAUGE_INCONSISTENCY, inconsistency, AtomicLong::doubleValue)
                .description("零件派生 deviceCategory 与 MDM 节点类别不一致条数")
                .register(meterRegistry);
    }

    /**
     * 刷新不一致条数（由定时巡检写入）
     *
     * @param total 不一致条数
     */
    public void updateInconsistency(long total) {
        inconsistency.set(Math.max(total, 0L));
    }

    /**
     * 当前不一致条数（最近一次巡检采样值）
     *
     * @return 不一致条数
     */
    public long getInconsistency() {
        return inconsistency.get();
    }

    /**
     * 记录一次修复批量结果
     *
     * @param trigger  触发来源（MANUAL / NODE_CHANGED / SCHEDULED）
     * @param applied  成功条数
     * @param skipped  已一致跳过条数
     * @param conflict 版本冲突条数
     * @param failed   失败条数
     */
    public void recordRepair(String trigger, int applied, int skipped, int conflict, int failed) {
        increment(trigger, "APPLIED", applied);
        increment(trigger, "SKIPPED", skipped);
        increment(trigger, "CONFLICT", conflict);
        increment(trigger, "FAILED", failed);
    }

    /**
     * 记录自动收敛整体异常（best-effort 路径，未落库任何修复）
     *
     * @param trigger 触发来源
     */
    public void recordRepairFailure(String trigger) {
        counter(COUNTER_REPAIR_FAILED, "trigger", nvl(trigger)).increment();
    }

    private void increment(String trigger, String result, int count) {
        if (count <= 0) {
            return;
        }
        counter(COUNTER_REPAIR, "trigger", nvl(trigger), "result", result).increment(count);
    }

    private Counter counter(String name, String... tags) {
        String key = name + "|" + String.join(",", tags);
        return counterCache.computeIfAbsent(key, k -> Counter.builder(name).tags(tags).register(meterRegistry));
    }

    private static String nvl(String value) {
        return value == null ? "UNKNOWN" : value;
    }
}
