package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.framework.security.crypto.KeyProvisioningTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.transit.TransitDerivedKeyReadiness;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * DerivedKeyProviderReadinessGate 单元测试（VMD-DSN-CR-061 §10 / VMD-DSN-CR-064 §5）。
 * <p>
 * CR-064：DERIVED_ROOT readiness 聚合改为 {@link TransitDerivedKeyReadiness}
 * （不再探测 legacy {@code /v1/business-key/capabilities}）；Provider 缺失或未就绪一律
 * fail-closed，禁止回退随机 Create / legacy Provider。
 *
 * @author hwyz_leo
 * @since 2026-10-22
 */
@ExtendWith(MockitoExtension.class)
class DerivedKeyProviderReadinessGateTest {

    @SuppressWarnings("unchecked")
    @Mock
    private ObjectProvider<KeyProvisioningTemplate> templateProvider;
    @SuppressWarnings("unchecked")
    @Mock
    private ObjectProvider<TransitDerivedKeyReadiness> readinessProvider;
    @Mock
    private KeyProvisioningTemplate template;
    @Mock
    private TransitDerivedKeyReadiness transitReadiness;

    private DerivedKeyProviderReadinessGate gate;

    @BeforeEach
    void setUp() {
        gate = new DerivedKeyProviderReadinessGate(templateProvider, readinessProvider);
    }

    private void requireReadyAtStartup(boolean value) {
        ReflectionTestUtils.setField(gate, "requireReadyAtStartup", value);
    }

    @Test
    @DisplayName("KeyProvisioningTemplate 未装配 → 未就绪（fail-closed）")
    void isReady_templateMissing_false() {
        when(templateProvider.getIfAvailable()).thenReturn(null);
        assertFalse(gate.isReady());
        assertTrue(gate.reason().contains("KeyProvisioningTemplate 未装配"));
    }

    @Test
    @DisplayName("未强制 READY（dev/test）→ 就绪 SKIPPED，不摘流")
    void isReady_notRequired_true() {
        requireReadyAtStartup(false);
        when(templateProvider.getIfAvailable()).thenReturn(template);
        assertTrue(gate.isReady());
        assertFalse(gate.isRequireReadyAtStartup());
        assertTrue(gate.reason().contains("require-ready-at-startup=false"));
    }

    @Test
    @DisplayName("强制 READY 但 TransitDerivedKeyReadiness 未装配 → 未就绪（provider 未选 local-wrap）")
    void isReady_readinessMissing_false() {
        requireReadyAtStartup(true);
        when(templateProvider.getIfAvailable()).thenReturn(template);
        when(readinessProvider.getIfAvailable()).thenReturn(null);
        assertFalse(gate.isReady());
        assertTrue(gate.reason().contains("TransitDerivedKeyReadiness 未装配"));
    }

    @Test
    @DisplayName("强制 READY 且 Transit 聚合就绪 → 就绪，reason 上报 READY")
    void isReady_readinessReady_true() {
        requireReadyAtStartup(true);
        when(templateProvider.getIfAvailable()).thenReturn(template);
        when(readinessProvider.getIfAvailable()).thenReturn(transitReadiness);
        when(transitReadiness.isReady()).thenReturn(true);
        when(transitReadiness.notReadyReason()).thenReturn(null);
        assertTrue(gate.isReady());
        assertTrue(gate.reason().contains("READY"));
    }

    @Test
    @DisplayName("强制 READY 但 Transit 聚合未就绪 → 未就绪，reason 委派 FW-SEC 稳定 reason")
    void isReady_readinessNotReady_false() {
        requireReadyAtStartup(true);
        when(templateProvider.getIfAvailable()).thenReturn(template);
        when(readinessProvider.getIfAvailable()).thenReturn(transitReadiness);
        when(transitReadiness.isReady()).thenReturn(false);
        when(transitReadiness.notReadyReason()).thenReturn("TRANSIT_HMAC_FORBIDDEN: HMAC 路径权限不足");
        assertFalse(gate.isReady());
        assertTrue(gate.reason().contains("TRANSIT_HMAC_FORBIDDEN"));
    }
}
