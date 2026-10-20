package net.hwyz.iov.cloud.edd.vmd.service.integration;

import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.CertificateApplyCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CertificateApplyResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.CertificateProvisioningAppService;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateCsrKeyUsageInvalidException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateCsrPublicKeyNotAllowedException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateIssuedProfileMismatchException;
import net.hwyz.iov.cloud.edd.vmd.service.support.TestTboxEcdhCsr;
import net.hwyz.iov.cloud.framework.security.crypto.CertEnrollmentTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.TboxDeviceCertificateValidator;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateProfileRejectedException;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateProfileRejectReason;
import net.hwyz.iov.cloud.framework.security.crypto.model.CertApplyResult;
import net.hwyz.iov.cloud.framework.security.crypto.model.DeviceCertificateProfile;
import net.hwyz.iov.cloud.framework.security.crypto.model.EnrollmentState;
import net.hwyz.iov.cloud.framework.security.crypto.model.IssuedCertificate;
import net.hwyz.iov.cloud.framework.security.crypto.model.ValidatedTboxDeviceCertificate;
import net.hwyz.iov.cloud.framework.security.crypto.resolver.DeviceCertificateRef;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;

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
 * VMD-DSN-CR-063 集成测试（真实 dev MySQL + {@code @MockBean CertEnrollmentTemplate/TboxDeviceCertificateValidator}）。
 * <p>
 * 覆盖：逻辑 v1 Profile 合法 CSR（P-256 / DS+KA / clientAuth / CN=UID）签发 → 签发后复验通过 →
 * ISSUED_NOT_CONFIRMED + leaf_certificate_digest 登记；CA 返回错误曲线/KU 或复验拒绝 → FAILED 不 ACTIVE；
 * CSR 双用途门禁拒绝且 enrollment 调用 0；同 requestId 幂等复用。
 *
 * @author hwyz_leo
 * @since 2026-10-20
 */
@Rollback
@TestPropertySource(properties = "vmd.certificate.profiles.tbox-device-ecdh-es-p256-v1.enabled=true")
class CertificateCr063IntegrationTest extends BaseTest {

    private static final String REQUEST_ID = "CR063_REQ_001";
    private static final String VIN = "HWYZTESTCR063001";
    private static final String DEVICE_SN = "TBOX-UID-CR063-001";
    /** 权威 HSM UID（CSR CN 与 leaf Subject CN 一致） */
    private static final String HSM_UID = "TBOX-UID-CR063";
    private static final String PART_CODE = "CR063_TBOX";
    private static final String PKI_REQUEST_ID = "PKI-CR063-001";
    private static final String V1_PROFILE = "TBOX_DEVICE_ECDH_ES_P256_V1";

    @MockBean
    private CertEnrollmentTemplate certificateEnrollmentTemplate;

    @MockBean
    private TboxDeviceCertificateValidator tboxDeviceCertificateValidator;

    @Autowired
    private CertificateProvisioningAppService certificateProvisioningAppService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private TestTboxEcdhCsr.CsrWithKey csrWithKey;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM tb_veh_certificate WHERE request_id LIKE 'CR063_REQ%'");
        jdbcTemplate.execute("DELETE FROM tb_vehicle_part WHERE vin = '" + VIN + "'");
        jdbcTemplate.execute("DELETE FROM tb_part_info WHERE sn = '" + DEVICE_SN + "'");
        jdbcTemplate.execute("DELETE FROM tb_veh_basic_info WHERE vin = '" + VIN + "'");

        jdbcTemplate.update(
                "INSERT INTO tb_veh_basic_info (vin, brand_code, platform_code, car_line_code, model_code, eol_time) " +
                        "VALUES (?, 'BRAND', 'PLAT', 'SER', 'MODEL', NOW())", VIN);
        jdbcTemplate.update(
                "INSERT INTO tb_part_info (part_code, sn, instance_state, extra) VALUES (?, ?, 1, ?)",
                PART_CODE, DEVICE_SN, "{\"hsm\":\"" + HSM_UID + "\"}");
        jdbcTemplate.update(
                "INSERT INTO tb_vehicle_part (vin, part_id, bind_state) SELECT ?, id, 1 FROM tb_part_info WHERE sn = ?",
                VIN, DEVICE_SN);

        csrWithKey = TestTboxEcdhCsr.validWithKey(HSM_UID);
        when(tboxDeviceCertificateValidator.validate(any(DeviceCertificateRef.class),
                eq(DeviceCertificateProfile.TBOX_DEVICE_ECDH_ES_P256_V1)))
                .thenReturn(new ValidatedTboxDeviceCertificate(
                        DeviceCertificateProfile.TBOX_DEVICE_ECDH_ES_P256_V1,
                        DeviceCertificateRef.ofSerial("CR063-SERIAL"), csrWithKey.keyPair().getPublic(), new byte[32],
                        true, true, true, true, Instant.now(), Instant.now()));
    }

    private CertificateApplyCmd cmd(String requestId, String csr) {
        return CertificateApplyCmd.builder()
                .requestId(requestId)
                .vin(VIN)
                .deviceCategory("TBOX")
                .deviceSn(DEVICE_SN)
                .certificateProfile(V1_PROFILE)
                .csrDerBase64(csr)
                .sourceSystem("MES")
                .facilityNo("FA-01")
                .lineCode("LINE-A")
                .build();
    }

    private IssuedCertificate leafFromKeyPair(KeyPair keyPair, String cn) {
        try {
            Date notBefore = Date.from(Instant.now().minusSeconds(3600));
            Date notAfter = Date.from(Instant.now().plusSeconds(24 * 3600));
            JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                    new X500Name("CN=TBOX-CA-TEST"), BigInteger.valueOf(System.nanoTime()), notBefore, notAfter,
                    new X500Name("CN=" + cn + ",O=OpenIOV,C=CN"), keyPair.getPublic());
            X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(
                    builder.build(new JcaContentSignerBuilder("SHA256withECDSA").build(keyPair.getPrivate())));
            byte[] der = certificate.getEncoded();
            return new IssuedCertificate(der, List.of(der), "CR063-SERIAL",
                    Instant.now(), Instant.now().plusSeconds(365L * 24 * 3600), "SHA256:FINGERPRINT");
        } catch (Exception e) {
            throw new RuntimeException("生成测试叶子证书失败", e);
        }
    }

    @Test
    @DisplayName("合法 v1 CSR：签发后复验通过 → ISSUED_NOT_CONFIRMED + leaf digest 登记")
    void applyDeviceCertificate_合法v1CSR_应复验通过并登记leafDigest() {
        // Given
        IssuedCertificate issued = leafFromKeyPair(csrWithKey.keyPair(), HSM_UID);
        when(certificateEnrollmentTemplate.apply(any())).thenReturn(
                new CertApplyResult(PKI_REQUEST_ID, EnrollmentState.ISSUED, Instant.now()));
        when(certificateEnrollmentTemplate.getCertificate(PKI_REQUEST_ID)).thenReturn(issued);

        // When
        CertificateApplyResult result = certificateProvisioningAppService.applyDeviceCertificate(cmd(REQUEST_ID, csrWithKey.csrDerBase64()));

        // Then
        assertEquals("ISSUED_NOT_CONFIRMED", result.getStatus());
        byte[] digest = jdbcTemplate.queryForObject(
                "SELECT leaf_certificate_digest FROM tb_veh_certificate WHERE request_id = ?", byte[].class, REQUEST_ID);
        assertNotNull(digest);
        assertEquals(32, digest.length);
        // leaf digest == SHA-256(leaf DER)
        assertArrayEquals(digest, sha256(issued.leafCertificate()));
        java.util.Date profileValidatedAt = jdbcTemplate.queryForObject(
                "SELECT profile_validated_at FROM tb_veh_certificate WHERE request_id = ?",
                java.util.Date.class, REQUEST_ID);
        assertNotNull(profileValidatedAt);
        verify(tboxDeviceCertificateValidator).validate(any(DeviceCertificateRef.class),
                eq(DeviceCertificateProfile.TBOX_DEVICE_ECDH_ES_P256_V1));
    }

    @Test
    @DisplayName("FW-SEC 复验拒绝 → FAILED，不 ACTIVE")
    void applyDeviceCertificate_FW复验拒绝_应FAILED不ACTIVE() {
        // Given：PKI 返回的证书虽 SPKI/CN 一致，但 FW validator 拒绝（KU 不满足）
        IssuedCertificate issued = leafFromKeyPair(csrWithKey.keyPair(), HSM_UID);
        when(certificateEnrollmentTemplate.apply(any())).thenReturn(
                new CertApplyResult(PKI_REQUEST_ID, EnrollmentState.ISSUED, Instant.now()));
        when(certificateEnrollmentTemplate.getCertificate(PKI_REQUEST_ID)).thenReturn(issued);
        when(tboxDeviceCertificateValidator.validate(any(), any()))
                .thenThrow(new CertificateProfileRejectedException(
                        CertificateProfileRejectReason.KEY_AGREEMENT_MISSING, "keyAgreement missing"));

        // When & Then：806119，状态 FAILED，leaf digest 不登记
        CertificateIssuedProfileMismatchException ex = assertThrows(
                CertificateIssuedProfileMismatchException.class,
                () -> certificateProvisioningAppService.applyDeviceCertificate(cmd(REQUEST_ID, csrWithKey.csrDerBase64())));
        assertEquals("806119", ex.getErrorCode().getCode());
        String status = jdbcTemplate.queryForObject(
                "SELECT cert_status FROM tb_veh_certificate WHERE request_id = ?", String.class, REQUEST_ID);
        assertEquals("FAILED", status);
        Integer digestCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_veh_certificate WHERE request_id = ? AND leaf_certificate_digest IS NOT NULL",
                Integer.class, REQUEST_ID);
        assertEquals(0, digestCount);
    }

    @Test
    @DisplayName("CSR KU 非法（缺 keyAgreement）→ 806113，enrollment 调用 0")
    void applyDeviceCertificate_KU非法_应拒绝且enrollment调用0() {
        // Given：PKI apply 本会成功，但 CSR 门禁应先拦截
        when(certificateEnrollmentTemplate.apply(any())).thenReturn(
                new CertApplyResult(PKI_REQUEST_ID, EnrollmentState.ISSUED, Instant.now()));
        String badCsr = TestTboxEcdhCsr.kuMissingKeyAgreement(HSM_UID);

        // When & Then：806113，PKI 不应被调用
        CertificateCsrKeyUsageInvalidException ex = assertThrows(
                CertificateCsrKeyUsageInvalidException.class,
                () -> certificateProvisioningAppService.applyDeviceCertificate(cmd(REQUEST_ID, badCsr)));
        assertEquals("806113", ex.getErrorCode().getCode());
        verify(certificateEnrollmentTemplate, never()).apply(any());
    }

    @Test
    @DisplayName("CSR 非 P-256（RSA）→ 806112，enrollment 调用 0")
    void applyDeviceCertificate_RSA_应拒绝且enrollment调用0() {
        String badCsr = TestTboxEcdhCsr.rsa(HSM_UID);

        CertificateCsrPublicKeyNotAllowedException ex = assertThrows(
                CertificateCsrPublicKeyNotAllowedException.class,
                () -> certificateProvisioningAppService.applyDeviceCertificate(cmd(REQUEST_ID, badCsr)));
        assertEquals("806112", ex.getErrorCode().getCode());
        verify(certificateEnrollmentTemplate, never()).apply(any());
    }

    @Test
    @DisplayName("同 requestId 重放 → 幂等返回原结果，不重复签发")
    void applyDeviceCertificate_重放_应幂等返回() {
        IssuedCertificate issued = leafFromKeyPair(csrWithKey.keyPair(), HSM_UID);
        when(certificateEnrollmentTemplate.apply(any())).thenReturn(
                new CertApplyResult(PKI_REQUEST_ID, EnrollmentState.ISSUED, Instant.now()));
        when(certificateEnrollmentTemplate.getCertificate(PKI_REQUEST_ID)).thenReturn(issued);

        CertificateApplyResult first = certificateProvisioningAppService.applyDeviceCertificate(cmd(REQUEST_ID, csrWithKey.csrDerBase64()));
        verify(certificateEnrollmentTemplate, times(1)).apply(any());

        CertificateApplyResult second = certificateProvisioningAppService.applyDeviceCertificate(cmd(REQUEST_ID, csrWithKey.csrDerBase64()));

        assertEquals(first.getRequestId(), second.getRequestId());
        verify(certificateEnrollmentTemplate, times(1)).apply(any());
    }

    private byte[] sha256(byte[] data) {
        try {
            return java.security.MessageDigest.getInstance("SHA-256").digest(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
