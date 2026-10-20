package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.health;

import net.hwyz.iov.cloud.edd.vmd.service.application.service.BoundDeviceIdentityResolver;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.DeviceCertificateIdentityPolicy;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.IssuedCertificateVerifier;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.certificateprofile.CertificateProfileRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.certificateprofile.VmdCertificateProfile;
import net.hwyz.iov.cloud.framework.security.crypto.CertEnrollmentTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.KeyprovReadiness;
import net.hwyz.iov.cloud.framework.security.crypto.TboxDeviceCertificateValidator;
import net.hwyz.iov.cloud.framework.security.crypto.resolver.CertificateReferenceDirectory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * TboxEcdhCertificateHealthIndicator 单元测试（VMD-DSN-CR-063 §12，VMD_TBOX_ECDH_CERT_READY 八项）。
 *
 * @author hwyz_leo
 * @since 2026-10-20
 */
class TboxEcdhCertificateHealthTest {

    @SuppressWarnings("unchecked")
    private final ObjectProvider<CertEnrollmentTemplate> enrollmentProvider = mock(ObjectProvider.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<TboxDeviceCertificateValidator> validatorProvider = mock(ObjectProvider.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<KeyprovReadiness> readinessProvider = mock(ObjectProvider.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<CertificateReferenceDirectory> dirProvider = mock(ObjectProvider.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<JdbcTemplate> jdbcProvider = mock(ObjectProvider.class);

    private final CertificateProfileRegistry registry = mock(CertificateProfileRegistry.class);
    private final DeviceCertificateIdentityPolicy policy = mock(DeviceCertificateIdentityPolicy.class);
    private final BoundDeviceIdentityResolver resolver = mock(BoundDeviceIdentityResolver.class);
    private final IssuedCertificateVerifier verifier = mock(IssuedCertificateVerifier.class);

    private TboxEcdhCertificateHealthIndicator indicator() {
        TboxEcdhCertificateHealthIndicator i = new TboxEcdhCertificateHealthIndicator(
                registry, policy, resolver, verifier, enrollmentProvider, validatorProvider,
                readinessProvider, dirProvider, jdbcProvider);
        lenient().when(enrollmentProvider.getIfAvailable()).thenReturn(mock(CertEnrollmentTemplate.class));
        lenient().when(validatorProvider.getIfAvailable()).thenReturn(mock(TboxDeviceCertificateValidator.class));
        return i;
    }

    @Test
    void disabled_应报DISABLED且UP() {
        TboxEcdhCertificateHealthIndicator i = indicator();
        ReflectionTestUtils.setField(i, "ecdhV1Enabled", false);

        Health health = i.health();

        assertEquals("UP", health.getStatus().getCode());
        assertEquals("DISABLED", health.getDetails().get("state"));
    }

    @Test
    void enabled且全部就绪_应UP_READY() {
        when(registry.isEnabled(VmdCertificateProfile.TBOX_DEVICE_ECDH_ES_P256_V1)).thenReturn(true);
        KeyprovReadiness readiness = mock(KeyprovReadiness.class);
        when(readiness.keyprovEcdhEsV1Ready()).thenReturn(true);
        lenient().when(readinessProvider.getIfAvailable()).thenReturn(readiness);
        lenient().when(dirProvider.getIfAvailable()).thenReturn(mock(CertificateReferenceDirectory.class));
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        lenient().when(jdbcProvider.getIfAvailable()).thenReturn(jdbc);
        doNothing().when(jdbc).query(anyString(), org.mockito.ArgumentMatchers.any(org.springframework.jdbc.core.RowCallbackHandler.class));

        TboxEcdhCertificateHealthIndicator i = indicator();
        ReflectionTestUtils.setField(i, "ecdhV1Enabled", true);

        Health health = i.health();

        assertEquals("UP", health.getStatus().getCode());
        assertEquals("READY", health.getDetails().get("state"));
    }

    @Test
    void enabled但FWcapability未就绪_应DOWN() {
        when(registry.isEnabled(VmdCertificateProfile.TBOX_DEVICE_ECDH_ES_P256_V1)).thenReturn(true);
        KeyprovReadiness readiness = mock(KeyprovReadiness.class);
        when(readiness.keyprovEcdhEsV1Ready()).thenReturn(false);
        lenient().when(readinessProvider.getIfAvailable()).thenReturn(readiness);
        lenient().when(dirProvider.getIfAvailable()).thenReturn(mock(CertificateReferenceDirectory.class));
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        lenient().when(jdbcProvider.getIfAvailable()).thenReturn(jdbc);
        doNothing().when(jdbc).query(anyString(), org.mockito.ArgumentMatchers.any(org.springframework.jdbc.core.RowCallbackHandler.class));

        TboxEcdhCertificateHealthIndicator i = indicator();
        ReflectionTestUtils.setField(i, "ecdhV1Enabled", true);

        Health health = i.health();

        assertEquals("DOWN", health.getStatus().getCode());
        assertEquals("DOWN", health.getDetails().get("state"));
    }

    @Test
    void enabled但leafDigest列缺失_应DOWN() {
        when(registry.isEnabled(VmdCertificateProfile.TBOX_DEVICE_ECDH_ES_P256_V1)).thenReturn(true);
        KeyprovReadiness readiness = mock(KeyprovReadiness.class);
        when(readiness.keyprovEcdhEsV1Ready()).thenReturn(true);
        lenient().when(readinessProvider.getIfAvailable()).thenReturn(readiness);
        lenient().when(dirProvider.getIfAvailable()).thenReturn(mock(CertificateReferenceDirectory.class));
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        lenient().when(jdbcProvider.getIfAvailable()).thenReturn(jdbc);
        doThrow(new RuntimeException("Unknown column 'leaf_certificate_digest'"))
                .when(jdbc).query(anyString(), org.mockito.ArgumentMatchers.any(org.springframework.jdbc.core.RowCallbackHandler.class));

        TboxEcdhCertificateHealthIndicator i = indicator();
        ReflectionTestUtils.setField(i, "ecdhV1Enabled", true);

        Health health = i.health();

        assertEquals("DOWN", health.getStatus().getCode());
        assertEquals("DOWN", health.getDetails().get("state"));
    }
}
