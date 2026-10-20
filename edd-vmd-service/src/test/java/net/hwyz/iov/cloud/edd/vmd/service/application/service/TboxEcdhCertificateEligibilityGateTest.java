package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.application.service.certificateprofile.CertificateProfileRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.certificateprofile.VmdCertificateProfile;
import net.hwyz.iov.cloud.framework.security.crypto.ImportProofVerifier;
import net.hwyz.iov.cloud.framework.security.crypto.KeyprovReadiness;
import net.hwyz.iov.cloud.framework.security.crypto.codec.DerivedEnvelopeDigestVerifier;
import net.hwyz.iov.cloud.framework.security.crypto.resolver.CertificateLocator;
import net.hwyz.iov.cloud.framework.security.crypto.resolver.CertificateReferenceDirectory;
import net.hwyz.iov.cloud.framework.security.crypto.resolver.DeviceCertificateRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * TboxEcdhCertificateEligibilityGate 单元测试（VMD-DSN-CR-063 §10 / RD-063-5/6）。
 * <p>
 * 覆盖：enabled=false 整个新 ECDH-ES 路径关闭（可重试 NOT_READY，不回退旧 Profile）、
 * sign-only 存量证书 fail-closed（确定性拒绝）、leaf digest 缺失拒绝、证书目录不可解析、
 * FW capability 未就绪、CR-062 readiness 未就绪、全条件满足放行。
 *
 * @author hwyz_leo
 * @since 2026-10-20
 */
@ExtendWith(MockitoExtension.class)
class TboxEcdhCertificateEligibilityGateTest {

    private static final String CERT_SN = "CR063-CERT-0001";
    private static final byte[] LEAF_DIGEST = new byte[32];

    @Mock
    private CertificateProfileRegistry certificateProfileRegistry;
    @Mock
    private ObjectProvider<CertificateReferenceDirectory> certificateDirectoryProvider;
    @Mock
    private CertificateReferenceDirectory certificateDirectory;
    @Mock
    private ObjectProvider<KeyprovReadiness> keyprovReadinessProvider;
    @Mock
    private KeyprovReadiness keyprovReadiness;
    @Mock
    private DerivedKeyProviderReadinessGate derivedKeyProviderReadinessGate;
    @Mock
    private ObjectProvider<DerivedEnvelopeDigestVerifier> envelopeDigestVerifierProvider;
    @Mock
    private ObjectProvider<ImportProofVerifier> importProofVerifierProvider;

    private TboxEcdhCertificateEligibilityGate gate;

    @BeforeEach
    void setUp() {
        lenient().when(certificateDirectoryProvider.getIfAvailable()).thenReturn(certificateDirectory);
        lenient().when(keyprovReadinessProvider.getIfAvailable()).thenReturn(keyprovReadiness);
        lenient().when(envelopeDigestVerifierProvider.getIfAvailable()).thenReturn(mock(DerivedEnvelopeDigestVerifier.class));
        lenient().when(importProofVerifierProvider.getIfAvailable()).thenReturn(mock(ImportProofVerifier.class));
        gate = new TboxEcdhCertificateEligibilityGate(certificateProfileRegistry, certificateDirectoryProvider,
                keyprovReadinessProvider, derivedKeyProviderReadinessGate, envelopeDigestVerifierProvider,
                importProofVerifierProvider);
    }

    private ActiveDeviceCertificate v1Active() {
        return new ActiveDeviceCertificate(CERT_SN, "PKI-REQ-1", "fingerprint", LEAF_DIGEST,
                VmdCertificateProfile.TBOX_DEVICE_ECDH_ES_P256_V1.name());
    }

    @Test
    void profile未启用_应关闭整个新ECDHES路径_可重试() {
        when(certificateProfileRegistry.isEnabled(VmdCertificateProfile.TBOX_DEVICE_ECDH_ES_P256_V1))
                .thenReturn(false);

        TboxEcdhCertificateEligibilityGate.GateResult result = gate.evaluate(v1Active());

        assertFalse(result.eligible());
        assertTrue(result.retryable(), "enabled=false 应可重试 NOT_READY，而非确定性拒绝");
    }

    @Test
    void signOnly存量证书_应确定性拒绝() {
        when(certificateProfileRegistry.isEnabled(VmdCertificateProfile.TBOX_DEVICE_ECDH_ES_P256_V1))
                .thenReturn(true);
        ActiveDeviceCertificate legacy = new ActiveDeviceCertificate(CERT_SN, "PKI-REQ-1", "f".repeat(64),
                null, "TBOX_TSP_CLIENT");

        TboxEcdhCertificateEligibilityGate.GateResult result = gate.evaluate(legacy);

        assertFalse(result.eligible());
        assertFalse(result.retryable(), "sign-only 存量证书对新 ECDH-ES 应确定性拒绝（RD-063-5）");
    }

    @Test
    void leafDigest缺失_应确定性拒绝() {
        when(certificateProfileRegistry.isEnabled(VmdCertificateProfile.TBOX_DEVICE_ECDH_ES_P256_V1))
                .thenReturn(true);
        ActiveDeviceCertificate noDigest = new ActiveDeviceCertificate(CERT_SN, "PKI-REQ-1", "f".repeat(64),
                null, VmdCertificateProfile.TBOX_DEVICE_ECDH_ES_P256_V1.name());

        TboxEcdhCertificateEligibilityGate.GateResult result = gate.evaluate(noDigest);

        assertFalse(result.eligible());
        assertFalse(result.retryable());
    }

    @Test
    void 证书目录不可解析_应可重试() {
        when(certificateProfileRegistry.isEnabled(VmdCertificateProfile.TBOX_DEVICE_ECDH_ES_P256_V1))
                .thenReturn(true);
        when(certificateDirectory.locate(any(DeviceCertificateRef.class))).thenReturn(Optional.empty());

        TboxEcdhCertificateEligibilityGate.GateResult result = gate.evaluate(v1Active());

        assertFalse(result.eligible());
        assertTrue(result.retryable());
    }

    @Test
    void FWcapability未就绪_应可重试() {
        when(certificateProfileRegistry.isEnabled(VmdCertificateProfile.TBOX_DEVICE_ECDH_ES_P256_V1))
                .thenReturn(true);
        when(certificateDirectory.locate(any(DeviceCertificateRef.class)))
                .thenReturn(Optional.of(new CertificateLocator.EnrollmentRequest("PKI-REQ-1")));
        when(keyprovReadiness.keyprovEcdhEsV1Ready()).thenReturn(false);

        TboxEcdhCertificateEligibilityGate.GateResult result = gate.evaluate(v1Active());

        assertFalse(result.eligible());
        assertTrue(result.retryable());
    }

    @Test
    void CR062readiness未就绪_应可重试() {
        when(certificateProfileRegistry.isEnabled(VmdCertificateProfile.TBOX_DEVICE_ECDH_ES_P256_V1))
                .thenReturn(true);
        when(certificateDirectory.locate(any(DeviceCertificateRef.class)))
                .thenReturn(Optional.of(new CertificateLocator.EnrollmentRequest("PKI-REQ-1")));
        when(keyprovReadiness.keyprovEcdhEsV1Ready()).thenReturn(true);
        when(derivedKeyProviderReadinessGate.isReady()).thenReturn(false);

        TboxEcdhCertificateEligibilityGate.GateResult result = gate.evaluate(v1Active());

        assertFalse(result.eligible());
        assertTrue(result.retryable());
    }

    @Test
    void 全条件满足_应放行() {
        when(certificateProfileRegistry.isEnabled(VmdCertificateProfile.TBOX_DEVICE_ECDH_ES_P256_V1))
                .thenReturn(true);
        when(certificateDirectory.locate(any(DeviceCertificateRef.class)))
                .thenReturn(Optional.of(new CertificateLocator.EnrollmentRequest("PKI-REQ-1")));
        when(keyprovReadiness.keyprovEcdhEsV1Ready()).thenReturn(true);
        when(derivedKeyProviderReadinessGate.isReady()).thenReturn(true);

        TboxEcdhCertificateEligibilityGate.GateResult result = gate.evaluate(v1Active());

        assertTrue(result.eligible());
    }
}
