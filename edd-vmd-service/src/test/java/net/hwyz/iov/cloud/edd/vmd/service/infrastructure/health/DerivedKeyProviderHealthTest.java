package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.health;

import net.hwyz.iov.cloud.edd.vmd.service.application.service.DerivedKeyProviderReadinessGate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * DerivedKeyProviderHealthIndicator 单元测试（VMD-DSN-CR-061 §10）。
 *
 * @author hwyz_leo
 * @since 2026-10-17
 */
class DerivedKeyProviderHealthTest {

    private final DerivedKeyProviderReadinessGate readinessGate = mock(DerivedKeyProviderReadinessGate.class);

    private final DerivedKeyProviderHealthIndicator indicator = new DerivedKeyProviderHealthIndicator(readinessGate);

    @Test
    @DisplayName("未强制 READY（dev/test）→ UP/SKIPPED，不摘流")
    void skippedWhenNotRequired() {
        when(readinessGate.isRequireReadyAtStartup()).thenReturn(false);
        when(readinessGate.reason()).thenReturn("ready（require-ready-at-startup=false）");
        Health health = indicator.health();
        assertEquals("UP", health.getStatus().getCode());
        assertEquals("SKIPPED", health.getDetails().get("state"));
    }

    @Test
    @DisplayName("强制 READY 但根派生 Provider 未就绪 → DOWN")
    void downWhenNotReady() {
        when(readinessGate.isRequireReadyAtStartup()).thenReturn(true);
        when(readinessGate.isReady()).thenReturn(false);
        when(readinessGate.reason()).thenReturn("Transit v2c-comm-root 未就绪");
        Health health = indicator.health();
        assertEquals("DOWN", health.getStatus().getCode());
        assertEquals("DOWN", health.getDetails().get("state"));
    }

    @Test
    @DisplayName("强制 READY 且已就绪 → UP/READY")
    void upWhenReady() {
        when(readinessGate.isRequireReadyAtStartup()).thenReturn(true);
        when(readinessGate.isReady()).thenReturn(true);
        when(readinessGate.reason()).thenReturn("ready");
        Health health = indicator.health();
        assertEquals("UP", health.getStatus().getCode());
        assertEquals("READY", health.getDetails().get("state"));
    }
}
