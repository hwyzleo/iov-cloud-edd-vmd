package net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 业务密钥状态值对象
 * <p>
 * CR-055：VMD 业务密钥目录的生命周期状态。VMD 权威维护，framework/KMS 不感知。
 * <ul>
 *   <li>PENDING：新行占位（material 创建前/中）</li>
 *   <li>ACTIVE：当前生效，可加密/解密</li>
 *   <li>DEPRECATED：已轮换，仅 decrypt_until 窗口内可解密</li>
 *   <li>REVOKING：吊销中，立即阻断新加解密</li>
 *   <li>REVOKED：已吊销，彻底不可用</li>
 *   <li>EXPIRED：超过 decrypt_until 过期</li>
 *   <li>FAILED：创建失败（可重试/补偿）</li>
 *   <li>RECONCILE_REQUIRED：framework 结果未知，需以原幂等键对账</li>
 *   <li>PENDING_RECOVERY：FAILED 行恢复的中间态（CR-066 §6.4），同事务内 FAILED→PENDING_RECOVERY→PENDING</li>
 *   <li>SUPERSEDED：被 canonical 目录行取代（CR-066 §6.4），终态不可用，保留证据元数据</li>
 * </ul>
 * <p>
 * <strong>CR-066 状态语义</strong>：{@link #SUPERSEDED} 为终态、不可用状态，不参与 ACTIVE/PENDING、
 * 轮换候选、目录解析与业务事件；占位行被 canonical 目录行 claim 后置 SUPERSEDED，必须同时记录
 * {@code superseded_by_id}，不得清空 requestId/operationId/failReason 等证据。{@link #PENDING_RECOVERY}
 * 为 FAILED→PENDING 恢复路径的中间态（同事务内可见，用于状态机一致性与审计）。
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@Getter
@AllArgsConstructor
public enum BusinessKeyState {

    PENDING("PENDING", "待激活"),
    ACTIVE("ACTIVE", "生效中"),
    DEPRECATED("DEPRECATED", "已弃用仅解密"),
    REVOKING("REVOKING", "吊销中"),
    REVOKED("REVOKED", "已吊销"),
    EXPIRED("EXPIRED", "已过期"),
    FAILED("FAILED", "创建失败"),
    RECONCILE_REQUIRED("RECONCILE_REQUIRED", "待对账"),
    PENDING_RECOVERY("PENDING_RECOVERY", "待恢复（FAILED恢复中间态）"),
    SUPERSEDED("SUPERSEDED", "已被canonical目录行取代（终态）");

    private final String value;
    private final String label;

    public static BusinessKeyState valOf(String val) {
        return Arrays.stream(BusinessKeyState.values())
                .filter(state -> state.value.equals(val))
                .findFirst()
                .orElse(null);
    }

    /**
     * 是否允许作为正向目录解析的 ACTIVE 结果。
     */
    public boolean isActive() {
        return this == ACTIVE;
    }

    /**
     * 是否允许解密（ACTIVE 或仍处于 decrypt_until 内的 DEPRECATED）。
     */
    public boolean canDecrypt() {
        return this == ACTIVE || this == DEPRECATED;
    }

    /**
     * 是否终态且不可用（CR-066）：SUPERSEDED/REVOKED/EXPIRED 不参与任何后续推进。
     * <p>FAILED/RECONCILE_REQUIRED 可恢复/可对账，PENDING/PENDING_RECOVERY 可推进，均非终态。
     */
    public boolean isTerminalUnusable() {
        return this == SUPERSEDED || this == REVOKED || this == EXPIRED;
    }

    /**
     * 是否可作为 claim-or-reuse 的 canonical 复用状态（CR-066 §6.3/§8）。
     * <ul>
     *   <li>ACTIVE：REIMPORT 收敛复用；</li>
     *   <li>PENDING / PENDING_RECOVERY：占位/恢复中间态 claim；</li>
     *   <li>FAILED / RECONCILE_REQUIRED：已有 keyId 证据，可恢复/对账后复用；</li>
     *   <li>其余状态（DEPRECATED/REVOKED/EXPIRED/SUPERSEDED）不可复用。
     * </ul>
     */
    public boolean reusableForClaim() {
        return this == ACTIVE || this == PENDING || this == PENDING_RECOVERY
                || this == FAILED || this == RECONCILE_REQUIRED;
    }
}
