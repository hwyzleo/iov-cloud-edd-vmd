package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 业务密钥目录 claim-or-reuse、代际决策与 keyprov 错误恢复元组监控指标（VMD-DSN-CR-066 §12 / CR-067 §9）。
 * <ul>
 *   <li>vmd_business_key_claim_total{path=update|reuse,outcome,reason}：目录材料回填/复用结果</li>
 *   <li>vmd_business_key_identity_conflict_total{reason}：同 keyId 不可变元数据冲突（不可重试）</li>
 *   <li>vmd_business_key_version_decision_total{decision=reimport|rotate|reject,reason}：代际决策</li>
 *   <li>vmd_keyprov_retry_loop_prevented_total{reason}：确定性死循环防断命中</li>
 *   <li>vmd_keyprov_error_total{phase=pre_batch|post_batch,protocol_code,action}：批次前后错误恢复元组（CR-067 §9）</li>
 *   <li>vmd_keyprov_recovery_total{mode=retry_request|query_status|do_not_retry,outcome}：恢复模式分发（CR-067 §9）</li>
 *   <li>vmd_keyprov_invalid_error_tuple_total{reason}：恢复元组不自洽命中（CR-067 §9）</li>
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

    /**
     * 记录 keyprov 错误恢复元组（CR-067 §9）：vmd_keyprov_error_total{phase,protocol_code,action}。
     * phase 仅区分 pre_batch/post_batch（由 protocol code 推导：ERR_RECONCILE_REQUIRED=post_batch）。
     *
     * @param phase        pre_batch / post_batch
     * @param protocolCode canonical ProtocolError.code（如 KEYPROV_VMD_UNAVAILABLE / KEYPROV_RECONCILE_REQUIRED）
     * @param action       RETRY_SAME_REQUEST_ID / QUERY_STATUS_LATER / DO_NOT_RETRY
     */
    public void recordKeyprovError(String phase, String protocolCode, String action) {
        counter("vmd_keyprov_error_total",
                "phase", nvl(phase), "protocol_code", nvl(protocolCode), "action", nvl(action)).increment();
    }

    /**
     * 记录恢复模式分发（CR-067 §9）：vmd_keyprov_recovery_total{mode,outcome}。
     *
     * @param mode   retry_request（UNAVAILABLE）/ query_status（OUTCOME_UNKNOWN）/ do_not_retry
     * @param outcome VmdKeyProvisioningOutcome 名称（低基数）
     */
    public void recordKeyprovRecovery(String mode, String outcome) {
        counter("vmd_keyprov_recovery_total", "mode", nvl(mode), "outcome", nvl(outcome)).increment();
    }

    /**
     * 记录恢复元组不自洽命中（CR-067 §9）：vmd_keyprov_invalid_error_tuple_total{reason}，
     * 对应告警：canonical 与 out-of-band 四元组不一致。
     *
     * @param reason reconcile_required_mismatch / retryable_unavailable_action_mismatch /
     *               not_found_must_not_retry / outcome_unknown_mismatch / unavailable_action_mismatch /
     *               rejected_action_mismatch 等
     */
    public void recordInvalidErrorTuple(String reason) {
        counter("vmd_keyprov_invalid_error_tuple_total", "reason", nvl(reason)).increment();
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
