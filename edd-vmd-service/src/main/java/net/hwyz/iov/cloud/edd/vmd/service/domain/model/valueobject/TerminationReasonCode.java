package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import java.util.Arrays;

/**
 * 滞留开通批次运维终止原因代码（VMD-DSN-CR-065 RD-065-3）
 * <p>
 * 受治理枚举：仅登记明确无副作用（或 Provider operation 已确认失败）的终止原因，
 * 自由文本只补充上下文、不作为状态机判断输入。终止码会写入批次 fail_reason，
 * 因此禁止包含换行、敏感材料或完整操作者上下文（后者由审计承载）。
 *
 * @author hwyz_leo
 * @since 2026-10-22
 */
public enum TerminationReasonCode {

    /** 设备证书由 F1 升级为 F2，旧批次摘要/收件证书与当前 ACTIVE 证书冲突，原摘要不可继续 */
    CERTIFICATE_CHANGED_DIGEST_CONFLICT("CERTIFICATE_CHANGED_DIGEST_CONFLICT", "证书已升级，旧批次摘要与当前ACTIVE证书冲突"),

    /** 确定性 Profile/KU 拒绝（sign-only→ECDH-ES），Provider 派生/封装提交前即拒绝，无副作用 */
    CERTIFICATE_PROFILE_REJECTED_STALE("CERTIFICATE_PROFILE_REJECTED_STALE", "证书Profile确定性拒绝，旧批次不可继续"),

    /** 依赖阻断且确认无副作用（如根派生/Transit 根长期不可用且 operation 未开始） */
    DEPENDENCY_BLOCKED_NO_SIDE_EFFECT("DEPENDENCY_BLOCKED_NO_SIDE_EFFECT", "依赖阻断且确认无成功副作用"),

    /** Provider operation 明确 FAILED/REJECTED/NOT_COMMITTED，经对账确认无成功副作用 */
    PROVIDER_OPERATION_CONFIRMED_FAILED("PROVIDER_OPERATION_CONFIRMED_FAILED", "Provider操作已确认失败"),

    /** 人工事故恢复：运维经安全调查确认可终止 */
    MANUAL_INCIDENT_RECOVERY("MANUAL_INCIDENT_RECOVERY", "人工事故恢复");

    private final String value;
    private final String label;

    TerminationReasonCode(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public static TerminationReasonCode valOf(String val) {
        if (val == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(code -> code.value.equals(val))
                .findFirst()
                .orElse(null);
    }
}
