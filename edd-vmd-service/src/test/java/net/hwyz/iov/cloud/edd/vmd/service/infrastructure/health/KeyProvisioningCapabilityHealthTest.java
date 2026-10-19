package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.health;

import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.adapter.KeyProvisioningServiceAdapter;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.BusinessKeyPolicyRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.DerivedKeyProviderReadinessGate;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.KeyprovConstants;
import net.hwyz.iov.cloud.framework.security.crypto.ImportProofVerifier;
import net.hwyz.iov.cloud.framework.security.crypto.codec.DerivedEnvelopeDigestVerifier;
import net.hwyz.iov.cloud.framework.security.crypto.resolver.CertificateReferenceDirectory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.health.AdditionalHealthEndpointPath;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpointGroup;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.boot.actuate.health.HttpCodeStatusMapper;
import org.springframework.boot.actuate.health.StatusAggregator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Keyprov capability 健康与分组测试（VMD-DSN-CR-059 §7）。
 *
 * @author hwyz_leo
 * @since 2026-10-16
 */
class KeyProvisioningCapabilityHealthTest {

    private ObjectProvider<KeyProvisioningServiceAdapter> adapterProvider;
    private BusinessKeyPolicyRegistry policyRegistry;
    private ObjectProvider<JdbcTemplate> jdbcProvider;
    private JdbcTemplate jdbcTemplate;
    private ObjectProvider<CertificateReferenceDirectory> certificateDirectoryProvider;
    private DerivedKeyProviderReadinessGate derivedKeyProviderReadinessGate;
    private ObjectProvider<DerivedEnvelopeDigestVerifier> envelopeVerifierProvider;
    private ObjectProvider<ImportProofVerifier> importProofVerifierProvider;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        adapterProvider = mock(ObjectProvider.class);
        policyRegistry = mock(BusinessKeyPolicyRegistry.class);
        jdbcProvider = mock(ObjectProvider.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        certificateDirectoryProvider = mock(ObjectProvider.class);
        derivedKeyProviderReadinessGate = mock(DerivedKeyProviderReadinessGate.class);
        envelopeVerifierProvider = mock(ObjectProvider.class);
        importProofVerifierProvider = mock(ObjectProvider.class);
    }

    private KeyProvisioningCapabilityHealthIndicator indicator(boolean enabled) {
        KeyProvisioningCapabilityHealthIndicator indicator =
                new KeyProvisioningCapabilityHealthIndicator(adapterProvider, policyRegistry, jdbcProvider,
                        certificateDirectoryProvider, derivedKeyProviderReadinessGate,
                        envelopeVerifierProvider, importProofVerifierProvider);
        ReflectionTestUtils.setField(indicator, "keyprovEnabled", enabled);
        ReflectionTestUtils.setField(indicator, "protocolRelease", "test-release-v1");
        ReflectionTestUtils.setField(indicator, "descriptorSetHash", "test-descriptor-set-hash");
        return indicator;
    }

    @Test
    @DisplayName("enabled=false → DISABLED 且 UP（非故障）")
    void disabledState() {
        Health health = indicator(false).health();
        assertEquals("UP", health.getStatus().getCode());
        assertEquals("DISABLED", health.getDetails().get("state"));
    }

    @Test
    @DisplayName("enabled=true 且能力齐备 → READY/UP")
    void readyState() {
        when(adapterProvider.getIfAvailable()).thenReturn(mock(KeyProvisioningServiceAdapter.class));
        when(certificateDirectoryProvider.getIfAvailable()).thenReturn(mock(CertificateReferenceDirectory.class));
        when(derivedKeyProviderReadinessGate.isReady()).thenReturn(true);
        when(envelopeVerifierProvider.getIfAvailable()).thenReturn(mock(DerivedEnvelopeDigestVerifier.class));
        when(importProofVerifierProvider.getIfAvailable()).thenReturn(mock(ImportProofVerifier.class));
        when(policyRegistry.resolvePurposes(KeyprovConstants.BUSINESS_DOMAIN_V2C))
                .thenReturn(Optional.of(KeyprovConstants.V2C_PURPOSE_SET));
        when(jdbcProvider.getIfAvailable()).thenReturn(jdbcTemplate);
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.ResultSetExtractor.class)))
                .thenReturn(null);

        Health health = indicator(true).health();
        assertEquals("UP", health.getStatus().getCode());
        assertEquals("READY", health.getDetails().get("state"));
    }

    @Test
    @DisplayName("enabled=true 但 adapter/证书目录/根派生Provider/策略缺失 → DOWN")
    void downState() {
        when(adapterProvider.getIfAvailable()).thenReturn(null);
        when(certificateDirectoryProvider.getIfAvailable()).thenReturn(null);
        when(derivedKeyProviderReadinessGate.isReady()).thenReturn(false);
        when(derivedKeyProviderReadinessGate.reason()).thenReturn("Transit v2c-comm-root 未就绪");
        when(envelopeVerifierProvider.getIfAvailable()).thenReturn(null);
        when(importProofVerifierProvider.getIfAvailable()).thenReturn(null);
        when(policyRegistry.resolvePurposes(KeyprovConstants.BUSINESS_DOMAIN_V2C))
                .thenReturn(Optional.of(Set.of()));
        when(jdbcProvider.getIfAvailable()).thenReturn(jdbcTemplate);
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.ResultSetExtractor.class)))
                .thenReturn(null);

        Health health = indicator(true).health();
        assertEquals("DOWN", health.getStatus().getCode());
        assertEquals("DOWN", health.getDetails().get("state"));
        assertEquals("DOWN", health.getDetails().get("adapter"));
        assertEquals("DOWN", health.getDetails().get("certificateDirectory"));
        assertEquals("DOWN", health.getDetails().get("derivedKeyProvider"));
        assertEquals("DOWN", health.getDetails().get("policy"));
    }

    @Test
    @DisplayName("自定义分组将 keyprov 从主聚合排除，但保留在 keyprov 分组")
    void healthGroupExclusion() {
        HealthEndpointGroup primary = stubGroup(name -> true);
        HealthEndpointGroup keyprovGroup = stubGroup(name -> KeyProvisioningHealthGroupsCustomizer.KEYPROV_INDICATOR.equals(name));
        HealthEndpointGroups groups = new HealthEndpointGroups() {
            @Override
            public HealthEndpointGroup getPrimary() {
                return primary;
            }

            @Override
            public Set<String> getNames() {
                return Set.of("keyprov");
            }

            @Override
            public HealthEndpointGroup get(String name) {
                return "keyprov".equals(name) ? keyprovGroup : null;
            }
        };

        HealthEndpointGroups processed = new KeyProvisioningHealthGroupsCustomizer()
                .postProcessHealthEndpointGroups(groups);

        assertFalse(processed.getPrimary().isMember(KeyProvisioningHealthGroupsCustomizer.KEYPROV_INDICATOR),
                "主聚合必须排除 keyprov");
        assertFalse(processed.getPrimary().isMember(KeyProvisioningHealthGroupsCustomizer.CERTIFICATE_RESOLUTION_INDICATOR),
                "主聚合必须排除 certificateResolution");
        assertFalse(processed.getPrimary().isMember(KeyProvisioningHealthGroupsCustomizer.DERIVED_KEY_PROVIDER_INDICATOR),
                "主聚合必须排除 derivedKeyProvider");
        assertFalse(processed.getPrimary().isMember(KeyProvisioningHealthGroupsCustomizer.KMS_BUSINESS_KEY_INDICATOR),
                "主聚合必须排除 kmsBusinessKey");
        assertFalse(processed.getPrimary().isMember(KeyProvisioningHealthGroupsCustomizer.IMPORT_PROOF_INDICATOR),
                "主聚合必须排除 importProofVerification");
        assertTrue(processed.getPrimary().isMember("db"), "其他指示器仍保留");
        assertTrue(processed.get("keyprov").isMember(KeyProvisioningHealthGroupsCustomizer.KEYPROV_INDICATOR),
                "keyprov 分组必须包含 capability");
        assertEquals(Set.of("keyprov"), processed.getNames());
    }

    private HealthEndpointGroup stubGroup(java.util.function.Predicate<String> member) {
        return new HealthEndpointGroup() {
            @Override
            public boolean isMember(String name) {
                return member.test(name);
            }

            @Override
            public boolean showComponents(SecurityContext securityContext) {
                return true;
            }

            @Override
            public boolean showDetails(SecurityContext securityContext) {
                return true;
            }

            @Override
            public StatusAggregator getStatusAggregator() {
                return mock(StatusAggregator.class);
            }

            @Override
            public HttpCodeStatusMapper getHttpCodeStatusMapper() {
                return mock(HttpCodeStatusMapper.class);
            }

            @Override
            public AdditionalHealthEndpointPath getAdditionalPath() {
                return null;
            }
        };
    }
}
