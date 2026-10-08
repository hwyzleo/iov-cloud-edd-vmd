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
 * </ul>
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
    RECONCILE_REQUIRED("RECONCILE_REQUIRED", "待对账");

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
}
