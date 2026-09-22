package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SecurityPresetDecision;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.VehicleNodeSchemaRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.monitoring.SecurityPresetMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SecurityPresetPolicy 判定策略单元测试（CR-049 §4.1）
 * <p>
 * 显式能力优先；null 走旧注册表兜底；未知枚举 INVALID_CAPABILITY（RD-049-1/2）。
 * </p>
 *
 * @author hwyz_leo
 */
@DisplayName("SecurityPresetPolicy 判定策略测试")
class SecurityPresetPolicyTest {

    private VehicleNodeSchemaRegistry legacyRegistry;
    private SecurityPresetMetrics metrics;
    private SecurityPresetPolicy policy;

    @BeforeEach
    void setUp() {
        legacyRegistry = new VehicleNodeSchemaRegistry();
        metrics = mock(SecurityPresetMetrics.class);
        policy = new SecurityPresetPolicy(legacyRegistry, metrics);
    }

    @Test
    @DisplayName("HSM_FULL 应判定为 PRESET_REQUIRED")
    void hsmFull_shouldRequirePreset() {
        assertEquals(SecurityPresetDecision.PRESET_REQUIRED, policy.decide("HSM_FULL", "CCU_GEN2"));
    }

    @Test
    @DisplayName("HSM_LIGHT 应判定为 PRESET_REQUIRED")
    void hsmLight_shouldRequirePreset() {
        assertEquals(SecurityPresetDecision.PRESET_REQUIRED, policy.decide("HSM_LIGHT", "BTM"));
    }

    @Test
    @DisplayName("NONE 应判定为 PRESET_NOT_REQUIRED")
    void none_shouldNotRequirePreset() {
        assertEquals(SecurityPresetDecision.PRESET_NOT_REQUIRED, policy.decide("NONE", "TBOX_5G"));
    }

    @Test
    @DisplayName("显式 SHE 应判定为 PRESET_NOT_REQUIRED，覆盖旧白名单（RD-049-1）")
    void she_shouldNotRequirePresetEvenIfInLegacyWhitelist() {
        // TBOX_5G 在旧注册表中需预置，但显式 SHE 必须关闭预置
        assertTrue(legacyRegistry.needsSecurityConstantPreset("TBOX_5G"));
        assertEquals(SecurityPresetDecision.PRESET_NOT_REQUIRED, policy.decide("SHE", "TBOX_5G"));
    }

    @Test
    @DisplayName("null 能力 + 旧注册表命中 → 兜底 REQUIRED + 指标 + warn")
    void nullCapability_shouldFallbackToLegacyRegistry() {
        assertEquals(SecurityPresetDecision.PRESET_REQUIRED, policy.decide(null, "TBOX_5G"));
        verify(metrics).recordLegacyFallback("TBOX_5G");
    }

    @Test
    @DisplayName("null 能力 + 旧注册表未命中 → 兜底 NOT_REQUIRED")
    void nullCapability_shouldSkipWhenNotInLegacyRegistry() {
        assertEquals(SecurityPresetDecision.PRESET_NOT_REQUIRED, policy.decide(null, "CAM_FRONT"));
        verify(metrics).recordLegacyFallback("CAM_FRONT");
    }

    @Test
    @DisplayName("未知枚举 → INVALID_CAPABILITY + 指标，不静默跳过")
    void unknownCapability_shouldReturnInvalid() {
        assertEquals(SecurityPresetDecision.INVALID_CAPABILITY, policy.decide("HSM_SUPER", "TBOX_5G"));
        verify(metrics).recordInvalidCapability("HSM_SUPER");
        verify(metrics, never()).recordLegacyFallback(any());
    }

    @Test
    @DisplayName("显式 NONE 应覆盖旧白名单（TBOX_5G 不再触发）")
    void explicitNone_shouldOverrideLegacyWhitelist() {
        assertTrue(legacyRegistry.needsSecurityConstantPreset("TBOX_5G"));
        assertEquals(SecurityPresetDecision.PRESET_NOT_REQUIRED, policy.decide("NONE", "TBOX_5G"));
    }
}
