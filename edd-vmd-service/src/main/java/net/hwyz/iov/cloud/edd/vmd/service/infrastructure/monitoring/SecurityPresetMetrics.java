package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 器件级安全常量预置监控指标（CR-049 §7）
 * <p>
 * 记录安全常量预置判定链路的关键指标：
 * - vmd.security_preset.decision_total{capability,decision}：判定结果（按能力 + 决策维度）
 * - vmd.security_preset.skip_total{reason,node_code}：明确不触发预置（NONE/SHE）及原因
 * - vmd.security_preset.legacy_fallback_total{node_code}：hsmCapability 缺失走旧注册表兜底
 * - vmd.security_preset.invalid_capability_total{raw_value}：未知枚举（契约错误）
 * - vmd.security_preset.biz_type_unresolved_total{device_category,node_code}：需预置但 BizType 不可解析
 * - vmd.mdm.vehicle_node.hsm_capability_missing_total{node_code}：投影缺失能力值
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-09-22
 */
@Slf4j
@Component
public class SecurityPresetMetrics {

    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> counterCache = new ConcurrentHashMap<>();

    public SecurityPresetMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * 记录判定结果
     *
     * @param capability 能力原始值（缺失用 "MISSING"）
     * @param decision   判定结果
     */
    public void recordDecision(String capability, String decision) {
        counter("vmd.security_preset.decision_total", "capability", nvl(capability), "decision", decision).increment();
    }

    /**
     * 记录明确跳过（NONE/SHE 等）
     *
     * @param reason   跳过原因
     * @param nodeCode 节点码
     */
    public void recordSkip(String reason, String nodeCode) {
        counter("vmd.security_preset.skip_total", "reason", nvl(reason), "node_code", nvl(nodeCode)).increment();
    }

    /**
     * 记录 hsmCapability 缺失走旧注册表兜底
     *
     * @param nodeCode 节点码
     */
    public void recordLegacyFallback(String nodeCode) {
        counter("vmd.security_preset.legacy_fallback_total", "node_code", nvl(nodeCode)).increment();
        counter("vmd.mdm.vehicle_node.hsm_capability_missing_total", "node_code", nvl(nodeCode)).increment();
    }

    /**
     * 记录未知枚举（契约错误）
     *
     * @param rawValue 原始能力值
     */
    public void recordInvalidCapability(String rawValue) {
        counter("vmd.security_preset.invalid_capability_total", "raw_value", nvl(rawValue)).increment();
    }

    /**
     * 记录需预置但 BizType 不可解析
     *
     * @param deviceCategory 设备类别
     * @param nodeCode       节点码
     */
    public void recordBizTypeUnresolved(String deviceCategory, String nodeCode) {
        counter("vmd.security_preset.biz_type_unresolved_total",
                "device_category", nvl(deviceCategory), "node_code", nvl(nodeCode)).increment();
    }

    private Counter counter(String name, String... tags) {
        return counterCache.computeIfAbsent(name + "|" + String.join("|", tags),
                k -> Counter.builder(name).tags(tags).register(meterRegistry));
    }

    private String nvl(String value) {
        return value == null || value.isBlank() ? "MISSING" : value;
    }
}
