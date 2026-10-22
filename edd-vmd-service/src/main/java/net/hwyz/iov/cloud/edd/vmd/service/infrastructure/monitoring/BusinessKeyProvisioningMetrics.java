package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 业务密钥目录 claim-or-reuse 与代际决策监控指标（VMD-DSN-CR-066 §12）。
 * <ul>
 *   <li>vmd_business_key_claim_total{path=update|reuse,outcome,reason}：目录材料回填/复用结果</li>
 *   <li>vmd_business_key_identity_conflict_total{reason}：同 keyId 不可变元数据冲突（不可重试）</li>
 *   <li>vmd_business_key_version_decision_total{decision=reimport|rotate|reject,reason}：代际决策</li>
 *   <li>vmd_keyprov_retry_loop_prevented_total{reason}：确定性死循环防断命中</li>
 * </ul>
 * 标签禁止 VIN、deviceSn、requestId、keyId、operationId、证书摘要或 KCV 等高基数标识。
 *
 * @author hwyz_leo
 * @since 2026-10-22
 */
@Slf4j
@Component
public class BusinessKeyProvisioningMetrics {

    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> counterCache = new ConcurrentHashMap<>();

    public BusinessKeyProvisioningMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * 记录目录材料回填/复用结果（CR-066 §6.3）。
     *
     * @param path    update（直接回填）/ reuse（claim-or-reuse 复用 canonical 行）
     * @param outcome success / identity_conflict / state_not_reusable / recovered
     * @param reason  低基数原因（非敏感，如 LOGICAL_KEY_ALREADY_CLAIMED / ROOT_MISMATCH / FAILED_EVIDENCE_INCOMPLETE）
     */
    public void recordClaim(String path, String outcome, String reason) {
        counter("vmd_business_key_claim_total",
                "path", nvl(path), "outcome", nvl(outcome), "reason", nvl(reason)).increment();
    }

    /**
     * 记录同 keyId 不可变元数据冲突（不可重试，告警触发）。
     *
     * @param reason 冲突维度（context_mismatch / root_mismatch / derivation_mismatch / kms_ref_mismatch /
     *               algorithm_mismatch / kcv_mismatch / policy_mismatch / state_not_reusable / evidence_incomplete）
     */
    public void recordIdentityConflict(String reason) {
        counter("vmd_business_key_identity_conflict_total", "reason", nvl(reason)).increment();
    }

    /**
     * 记录版本代际决策（CR-066 §6.1）。
     *
     * @param decision reimport / rotate / reject
     * @param reason   low-cardinality 决策原因（同代际复用 / 派生格式变化 / 声明版本冲突 / 无ACTIVE 等）
     */
    public void recordVersionDecision(String decision, String reason) {
        counter("vmd_business_key_version_decision_total",
                "decision", nvl(decision), "reason", nvl(reason)).increment();
    }

    /**
     * 记录确定性死循环防断命中（CR-066 §2/§8：确定性唯一键冲突不再落可重试 806100）。
     *
     * @param reason claim_converged / identity_conflict_rejected / version_generation_rejected 等
     */
    public void recordRetryLoopPrevented(String reason) {
        counter("vmd_keyprov_retry_loop_prevented_total", "reason", nvl(reason)).increment();
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
