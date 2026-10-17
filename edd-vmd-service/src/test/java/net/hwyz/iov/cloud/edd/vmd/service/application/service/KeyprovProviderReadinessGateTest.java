package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.framework.security.crypto.client.KmsBusinessKeyProviderReadiness;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * KeyprovProviderReadinessGate 单元测试（VMD-DSN-CR-060 RD-060-9 / §7）。
 *
 * @author hwyz_leo
 * @since 2026-10-17
 */
@ExtendWith(MockitoExtension.class)
class KeyprovProviderReadinessGateTest {

    @SuppressWarnings("unchecked")
    @Mock
    private ObjectProvider<KmsBusinessKeyProviderReadiness> readinessProvider;
    @Mock
    private KmsBusinessKeyProviderReadiness readiness;

    private KeyprovProviderReadinessGate gate;

    @BeforeEach
    void setUp() {
        gate = new KeyprovProviderReadinessGate(readinessProvider);
    }

    @Test
    @DisplayName("未装配 Provider → 未就绪（fail-closed）")
    void isReady_noBean_false() {
        when(readinessProvider.getIfAvailable()).thenReturn(null);
        assertFalse(gate.isReady());
        assertNotNull(gate.reason());
        assertTrue(gate.reason().contains("未装配"), "应说明 capability 未装配: " + gate.reason());
    }

    @Test
    @DisplayName("Provider READY → 就绪")
    void isReady_ready_true() {
        when(readinessProvider.getIfAvailable()).thenReturn(readiness);
        when(readiness.isReady()).thenReturn(true);
        assertTrue(gate.isReady());
    }

    @Test
    @DisplayName("Provider 未 READY → 未就绪")
    void isReady_notReady_false() {
        when(readinessProvider.getIfAvailable()).thenReturn(readiness);
        when(readiness.isReady()).thenReturn(false);
        assertFalse(gate.isReady());
    }

    @Test
    @DisplayName("reason 委派 Provider 未就绪原因")
    void reason_delegatesToProvider() {
        when(readinessProvider.getIfAvailable()).thenReturn(readiness);
        when(readiness.getReason()).thenReturn("capability path missing");
        assertEquals("capability path missing", gate.reason());
    }
}
