package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.HealthEndpointGroup;
import org.springframework.boot.actuate.health.HealthEndpointGroups;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * KeyProvisioningHealthGroupsCustomizer 单元测试（VMD-DSN-CR-059 §7 / VMD-DSN-CR-064 §5）。
 * <p>
 * CR-064：FW-SEC {@code transitDerivedKey} 指示器加入 keyprov 域排除集——未就绪报告
 * NOT_READY 时不得拖垮主 readiness/liveness 聚合；仍可在独立 keyprov 健康分组呈现。
 *
 * @author hwyz_leo
 * @since 2026-10-22
 */
class KeyProvisioningHealthGroupsCustomizerTest {

    private final KeyProvisioningHealthGroupsCustomizer customizer = new KeyProvisioningHealthGroupsCustomizer();

    @Test
    @DisplayName("transitDerivedKey 指示器注册名与 CR-064 契约一致")
    void indicatorNameContract() {
        assertEquals("transitDerivedKey", KeyProvisioningHealthGroupsCustomizer.TRANSIT_DERIVED_KEY_INDICATOR);
    }

    @Test
    @DisplayName("主聚合排除全部 keyprov 域指示器（含 transitDerivedKey），非 keyprov 指示器保留")
    void excludesKeyprovFromPrimary() {
        HealthEndpointGroup primary = mock(HealthEndpointGroup.class);
        when(primary.isMember(org.mockito.ArgumentMatchers.anyString())).thenReturn(true);
        HealthEndpointGroups groups = mock(HealthEndpointGroups.class);
        when(groups.getPrimary()).thenReturn(primary);

        HealthEndpointGroups processed = customizer.postProcessHealthEndpointGroups(groups);
        HealthEndpointGroup primaryOut = processed.getPrimary();

        assertFalse(primaryOut.isMember(KeyProvisioningHealthGroupsCustomizer.KEYPROV_INDICATOR));
        assertFalse(primaryOut.isMember(KeyProvisioningHealthGroupsCustomizer.DERIVED_KEY_PROVIDER_INDICATOR));
        assertFalse(primaryOut.isMember(KeyProvisioningHealthGroupsCustomizer.TRANSIT_DERIVED_KEY_INDICATOR));
        assertFalse(primaryOut.isMember(KeyProvisioningHealthGroupsCustomizer.CERTIFICATE_RESOLUTION_INDICATOR));
        assertFalse(primaryOut.isMember(KeyProvisioningHealthGroupsCustomizer.KMS_BUSINESS_KEY_INDICATOR));
        assertFalse(primaryOut.isMember(KeyProvisioningHealthGroupsCustomizer.IMPORT_PROOF_INDICATOR));
        assertFalse(primaryOut.isMember(KeyProvisioningHealthGroupsCustomizer.TBOX_ECDH_CERT_INDICATOR));
        // 非 keyprov 能力不受影响
        assertTrue(primaryOut.isMember("redis"));
        assertTrue(primaryOut.isMember("db"));
    }

    @Test
    @DisplayName("named group（keyprov）不被改写，保留原引用")
    void namedGroupsPreserved() {
        HealthEndpointGroup keyprovGroup = mock(HealthEndpointGroup.class);
        HealthEndpointGroups groups = mock(HealthEndpointGroups.class);
        when(groups.getPrimary()).thenReturn(mock(HealthEndpointGroup.class));
        when(groups.getNames()).thenReturn(java.util.Set.of("keyprov"));
        when(groups.get("keyprov")).thenReturn(keyprovGroup);

        HealthEndpointGroups processed = customizer.postProcessHealthEndpointGroups(groups);
        assertEquals(java.util.Set.of("keyprov"), processed.getNames());
        assertEquals(keyprovGroup, processed.get("keyprov"));
    }
}
