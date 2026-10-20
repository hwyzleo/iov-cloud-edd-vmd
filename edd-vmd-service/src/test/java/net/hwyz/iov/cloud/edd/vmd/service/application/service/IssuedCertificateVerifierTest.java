package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.application.service.certificateprofile.CertificateProfileRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.certificateprofile.RegisteredCertificateProfile;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.certificateprofile.VmdCertificateProfile;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateIssuedProfileMismatchException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificatePublicKeyBindingMismatchException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateUidBindingMismatchException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleCertificate;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.CertificateStatus;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.security.CsrUtils;
import net.hwyz.iov.cloud.edd.vmd.service.support.TestTboxEcdhCsr;
import net.hwyz.iov.cloud.framework.security.crypto.TboxDeviceCertificateValidator;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateProfileRejectedException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateProfileRejectReason;
import net.hwyz.iov.cloud.framework.security.crypto.model.CertificateProfile;
import net.hwyz.iov.cloud.framework.security.crypto.model.DeviceCertificateProfile;
import net.hwyz.iov.cloud.framework.security.crypto.model.IssuedCertificate;
import net.hwyz.iov.cloud.framework.security.crypto.model.ValidatedTboxDeviceCertificate;
import net.hwyz.iov.cloud.framework.security.crypto.resolver.CertificateSubjectRef;
import net.hwyz.iov.cloud.framework.security.crypto.resolver.DeviceCertificateRef;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * IssuedCertificateVerifier 单元测试（VMD-DSN-CR-063 §7 签发后复验）。
 * <p>
 * 覆盖：leaf SPKI==CSR SPKI、Subject CN==权威 UID、FW-SEC CR-017 复验语义
 * （p256/digitalSignature/keyAgreement/clientAuth 全真）、leaf digest=SHA-256(leaf DER) 登记、
 * 任一失败抛稳定异常且不置 ACTIVE；legacy Profile 跳过复验。
 *
 * @author hwyz_leo
 * @since 2026-10-20
 */
@ExtendWith(MockitoExtension.class)
class IssuedCertificateVerifierTest {

    private static final String HSM_UID = "00000000000000000000000000000001";
    private static final String V1_PROFILE = "TBOX_DEVICE_ECDH_ES_P256_V1";

    @Mock
    private CertificateProfileRegistry certificateProfileRegistry;

    @Mock
    private ObjectProvider<TboxDeviceCertificateValidator> validatorProvider;

    @Mock
    private TboxDeviceCertificateValidator validator;

    private IssuedCertificateVerifier verifier;
    private TestTboxEcdhCsr.CsrWithKey csrWithKey;

    @BeforeEach
    void setUp() {
        lenient().when(certificateProfileRegistry.requireEnabled(V1_PROFILE))
                .thenReturn(new RegisteredCertificateProfile(
                        VmdCertificateProfile.TBOX_DEVICE_ECDH_ES_P256_V1,
                        DeviceCertificateProfile.TBOX_DEVICE_ECDH_ES_P256_V1,
                        new CertificateProfile(V1_PROFILE, V1_PROFILE,
                                CertificateProfile.SubjectType.DEVICE_IDENTITY, "EC", "DIGITAL_SIGNATURE,KEY_AGREEMENT"),
                        "TBOX", true, true));
        lenient().when(validatorProvider.getIfAvailable()).thenReturn(validator);
        verifier = new IssuedCertificateVerifier(certificateProfileRegistry, validatorProvider);
        csrWithKey = TestTboxEcdhCsr.validWithKey(HSM_UID);
    }

    @Test
    void 复验通过_应登记leafDigest与复验时间() throws Exception {
        // Given：leaf 与 CSR 同 SPKI、CN=UID、validator 全真
        IssuedCertificate issued = leafFromKeyPair(csrWithKey.keyPair(), HSM_UID);
        VehicleCertificate cert = certWithPublicKey(csrWithKey.csrDerBase64());
        when(validator.validate(any(DeviceCertificateRef.class), eq(DeviceCertificateProfile.TBOX_DEVICE_ECDH_ES_P256_V1)))
                .thenReturn(validatedAllTrue());

        // When
        verifier.verifyPostIssuance(cert, issued);

        // Then：digest = SHA-256(leaf DER)，复验时间非空，状态未被改动（由调用方决定 ACTIVE）
        assertNotNull(cert.getLeafCertificateDigest());
        assertEquals(32, cert.getLeafCertificateDigest().length);
        assertNotNull(cert.getProfileValidatedAt());
        assertArrayEquals(sha256(issued.leafCertificate()), cert.getLeafCertificateDigest());
        verify(validator).validate(any(DeviceCertificateRef.class), eq(DeviceCertificateProfile.TBOX_DEVICE_ECDH_ES_P256_V1));
    }

    @Test
    void leafSPKI与CSR不一致_应拒绝806117() throws Exception {
        // Given：leaf 用另一把密钥
        KeyPair other = TestTboxEcdhCsr.validWithKey(HSM_UID).keyPair();
        IssuedCertificate issued = leafFromKeyPair(other, HSM_UID);
        VehicleCertificate cert = certWithPublicKey(csrWithKey.csrDerBase64());

        // When & Then
        CertificatePublicKeyBindingMismatchException ex = assertThrows(
                CertificatePublicKeyBindingMismatchException.class,
                () -> verifier.verifyPostIssuance(cert, issued));
        assertEquals("806117", ex.getErrorCode().getCode());
        verify(validator, never()).validate(any(), any());
    }

    @Test
    void leafSubjectCN与权威UID不一致_应拒绝806118() throws Exception {
        IssuedCertificate issued = leafFromKeyPair(csrWithKey.keyPair(), "00000000000000000000000000000009");
        VehicleCertificate cert = certWithPublicKey(csrWithKey.csrDerBase64());

        CertificateUidBindingMismatchException ex = assertThrows(
                CertificateUidBindingMismatchException.class,
                () -> verifier.verifyPostIssuance(cert, issued));
        assertEquals("806118", ex.getErrorCode().getCode());
        verify(validator, never()).validate(any(), any());
    }

    @Test
    void FW校验拒绝_应拒绝806119() throws Exception {
        IssuedCertificate issued = leafFromKeyPair(csrWithKey.keyPair(), HSM_UID);
        VehicleCertificate cert = certWithPublicKey(csrWithKey.csrDerBase64());
        when(validator.validate(any(), any())).thenThrow(new CertificateProfileRejectedException(
                CertificateProfileRejectReason.KEY_AGREEMENT_MISSING, "keyAgreement missing"));

        CertificateIssuedProfileMismatchException ex = assertThrows(
                CertificateIssuedProfileMismatchException.class,
                () -> verifier.verifyPostIssuance(cert, issued));
        assertEquals("806119", ex.getErrorCode().getCode());
    }

    @Test
    void FW校验语义不满足_应拒绝806119() throws Exception {
        IssuedCertificate issued = leafFromKeyPair(csrWithKey.keyPair(), HSM_UID);
        VehicleCertificate cert = certWithPublicKey(csrWithKey.csrDerBase64());
        when(validator.validate(any(), any())).thenReturn(validatedKeyAgreementFalse());

        assertThrows(CertificateIssuedProfileMismatchException.class,
                () -> verifier.verifyPostIssuance(cert, issued));
    }

    @Test
    void legacyProfile_应跳过复验且不调用validator() throws Exception {
        lenient().when(certificateProfileRegistry.requireEnabled("TBOX_TSP_CLIENT"))
                .thenReturn(new RegisteredCertificateProfile(
                        VmdCertificateProfile.TBOX_TSP_CLIENT, null,
                        new CertificateProfile("TBOX_TSP_CLIENT", "TBOX_TSP_CLIENT",
                                CertificateProfile.SubjectType.DEVICE_IDENTITY, "EC", "DIGITAL_SIGNATURE"),
                        "TBOX", false, true));
        IssuedCertificate issued = leafFromKeyPair(csrWithKey.keyPair(), HSM_UID);
        VehicleCertificate cert = certWithPublicKey(csrWithKey.csrDerBase64());
        cert.setCertificateProfile("TBOX_TSP_CLIENT");

        verifier.verifyPostIssuance(cert, issued);

        verify(validator, never()).validate(any(), any());
        assertNull(cert.getLeafCertificateDigest());
    }

    private VehicleCertificate certWithPublicKey(String csrDerBase64) {
        return VehicleCertificate.builder()
                .requestId("REQ-V1-001")
                .hsmUid(HSM_UID)
                .publicKeySha256(CsrUtils.calculatePublicKeySha256(csrDerBase64))
                .certificateProfile(V1_PROFILE)
                .certStatus(CertificateStatus.ISSUING)
                .build();
    }

    private ValidatedTboxDeviceCertificate validatedAllTrue() {
        return new ValidatedTboxDeviceCertificate(
                DeviceCertificateProfile.TBOX_DEVICE_ECDH_ES_P256_V1,
                DeviceCertificateRef.ofSubject(CertificateSubjectRef.ofDevice(HSM_UID)),
                csrWithKey.keyPair().getPublic(), new byte[32], true, true, true, true, Instant.now(), Instant.now());
    }

    private ValidatedTboxDeviceCertificate validatedKeyAgreementFalse() {
        return new ValidatedTboxDeviceCertificate(
                DeviceCertificateProfile.TBOX_DEVICE_ECDH_ES_P256_V1,
                DeviceCertificateRef.ofSubject(CertificateSubjectRef.ofDevice(HSM_UID)),
                csrWithKey.keyPair().getPublic(), new byte[32], true, true, false, true, Instant.now(), Instant.now());
    }

    /**
     * 用给定密钥对生成自签叶子证书（CN 可指定），返回 IssuedCertificate
     */
    private IssuedCertificate leafFromKeyPair(KeyPair keyPair, String cn) throws Exception {
        Date notBefore = Date.from(Instant.now().minusSeconds(3600));
        Date notAfter = Date.from(Instant.now().plusSeconds(24 * 3600));
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                new X500Name("CN=TBOX-CA-TEST"), BigInteger.valueOf(System.nanoTime()), notBefore, notAfter,
                new X500Name("CN=" + cn + ",O=OpenIOV,C=CN"), keyPair.getPublic());
        X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(
                builder.build(new JcaContentSignerBuilder("SHA256withECDSA").build(keyPair.getPrivate())));
        byte[] der = certificate.getEncoded();
        return new IssuedCertificate(der, List.of(der), "SERIAL-001",
                Instant.now(), Instant.now().plusSeconds(365L * 24 * 3600), "SHA256:FINGERPRINT");
    }

    private byte[] sha256(byte[] data) throws Exception {
        return java.security.MessageDigest.getInstance("SHA-256").digest(data);
    }
}
