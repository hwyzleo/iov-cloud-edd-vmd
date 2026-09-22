package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.HsmCapability;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SecurityPresetDecision;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehicleNodeSchemaRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.SecurityPresetMetrics;
import org.springframework.stereotype.Service;

/**
 * 安全常量预置资格判定策略（CR-049 §4.1）
 * <p>
 * 以 MDM VehicleNode.hsmCapability 主数据为权威来源（RD-049-1）：
 * - 显式能力优先，不再与节点码白名单做 OR 合并（否则 NONE/SHE 无法关闭预置）；
 * - null/缺失才允许访问旧注册表兜底，并输出 warn 与 legacy_fallback 指标；
 * - 未知枚举返回 INVALID_CAPABILITY，进入失败分支，不得降级为 false（RD-049-2）。
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-09-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityPresetPolicy {

    private final VehicleNodeSchemaRegistry legacyRegistry;
    private final SecurityPresetMetrics metrics;

    /**
     * 判定器件是否需要进行安全常量预置
     *
     * @param rawHsmCapability MDM 透传的 hsmCapability 原始值（可空）
     * @param legacyNodeCode   车载节点代码（兼容兜底用）
     * @return 判定结果
     */
    public SecurityPresetDecision decide(String rawHsmCapability, String legacyNodeCode) {
        HsmCapability capability;
        try {
            capability = HsmCapability.fromRawValue(rawHsmCapability);
        } catch (IllegalArgumentException e) {
            metrics.recordInvalidCapability(rawHsmCapability);
            log.error("车辆节点[{}]hsmCapability 非法: {}, 判定为 INVALID_CAPABILITY，不得静默跳过",
                    legacyNodeCode, rawHsmCapability);
            return SecurityPresetDecision.INVALID_CAPABILITY;
        }

        if (capability == null) {
            // 主数据缺失 → 兼容兜底：旧注册表白名单，可观测、可退场
            boolean legacy = legacyRegistry.needsSecurityConstantPreset(legacyNodeCode);
            metrics.recordLegacyFallback(legacyNodeCode);
            log.warn("车辆节点[{}]hsmCapability 缺失，走旧注册表兜底: needsPreset={}", legacyNodeCode, legacy);
            return legacy ? SecurityPresetDecision.PRESET_REQUIRED : SecurityPresetDecision.PRESET_NOT_REQUIRED;
        }

        SecurityPresetDecision decision = switch (capability) {
            case HSM_LIGHT, HSM_FULL -> SecurityPresetDecision.PRESET_REQUIRED;
            case NONE, SHE -> SecurityPresetDecision.PRESET_NOT_REQUIRED;
        };
        metrics.recordDecision(rawHsmCapability, decision.name());
        return decision;
    }
}
