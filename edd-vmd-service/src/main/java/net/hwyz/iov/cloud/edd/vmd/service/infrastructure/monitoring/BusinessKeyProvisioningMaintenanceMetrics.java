package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 滞留开通批次运维终止监控指标（VMD-DSN-CR-065 §7.2）
 * <ul>
 *   <li>vmd_business_key_provisioning_termination_total{from_state,reason,result}：终止执行结果</li>
 *   <li>vmd_business_key_provisioning_termination_blocked_total{reason}：门禁拒绝</li>
 *   <li>vmd_business_key_provisioning_stale{state}：滞留批次（维护扫描/终止时上报）</li>
 *   <li>vmd_business_key_pending_failed_total{reason=ops_termination}：运维终止连带失败化 PENDING 目录数</li>
 * </ul>
 * 标签禁止 VIN、deviceSn、operationId、证书标识、keyId、KCV、Wrapped Key 与凭据。
 *
 * @author hwyz_leo
 * @since 2026-10-22
 */
@Slf4j
@Component
public class BusinessKeyProvisioningMaintenanceMetrics {

    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> counterCache = new ConcurrentHashMap<>();

    public BusinessKeyProvisioningMaintenanceMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * 记录终止执行结果
     *
     * @param fromState 终止前状态
     * @param reason    终止原因码
     * @param result    success / blocked / idempotent
     */
    public void recordTermination(String fromState, String reason, String result) {
        counter("vmd_business_key_provisioning_termination_total",
                "from_state", nvl(fromState), "reason", nvl(reason), "result", nvl(result)).increment();
    }

    /**
     * 记录门禁拒绝
     *
     * @param reason 阻断原因（状态不允许/需先对账/成功事实/目录冲突/视图过期/未启用 等）
     */
    public void recordBlocked(String reason) {
        counter("vmd_business_key_provisioning_termination_blocked_total", "reason", nvl(reason)).increment();
    }

    /**
     * 记录滞留批次（state 维度，供监控长期滞留）
     *
     * @param state PREPARING / RECONCILE_REQUIRED / WAIT_DEVICE_ACK
     */
    public void recordStale(String state) {
        counter("vmd_business_key_provisioning_stale", "state", nvl(state)).increment();
    }

    /**
     * 记录运维终止连带失败化的 PENDING 目录数
     *
     * @param count 失败化目录数
     */
    public void recordPendingFailed(int count) {
        if (count <= 0) {
            return;
        }
        counter("vmd_business_key_pending_failed_total", "reason", "ops_termination").increment(count);
    }

    private Counter counter(String name, String... tags) {
        String key = name + "|" + String.join("|", tags);
        return counterCache.computeIfAbsent(key, k -> io.micrometer.core.instrument.Counter.builder(name)
                .tags(tags)
                .register(meterRegistry));
    }

    private String nvl(String value) {
        return value != null ? value : "unknown";
    }
}
