package net.hwyz.iov.cloud.edd.vmd.service.integration;

import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.CertificateApplyCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CertificateApplyResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CertificateStatusResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.CertificateProvisioningAppService;
import net.hwyz.iov.cloud.framework.security.crypto.CertEnrollmentTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.model.CertApplyResult;
import net.hwyz.iov.cloud.framework.security.crypto.model.EnrollmentState;
import net.hwyz.iov.cloud.framework.security.crypto.model.IssuedCertificate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;

import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * VMD-DSN-CR-044 集成测试
 * <p>
 * 覆盖：产线 TBOX 证书首次签发的端到端链路——VMD 校验装车绑定后经 framework 同步签发，
 * 响应携带证书 DER/链（供产线注入 TBOX，0x31 FF03 写入内容来源）、登记 tb_veh_certificate
 * （含从 DER 解析的 subject/issuer）、以及 ISSUED 后经 pki_request_id 重取回填证书本体。
 * </p>
 *
 * @author CR-044
 */
@Rollback
class CertificateCr044IntegrationTest extends BaseTest {

    private static final String REQUEST_ID = "CR044_REQ_001";
    private static final String VIN = "HWYZTESTCR044001";
    private static final String DEVICE_SN = "TBOX-UID-CR044-001";
    private static final String PART_CODE = "CR044_TBOX";
    private static final String PKI_REQUEST_ID = "PKI-CR044-001";
    private static final String CERT_SN = "CR044-CERT-001";
    /** 自签 X.509 证书（CN=TBOX-UID-000001,O=OpenIOV,C=CN）DER Base64，模拟 PKI 返回的叶子证书 */
    private static final String CERT_DER_BASE64 =
            "MIIDUzCCAjugAwIBAgIUBVmSTuBfOQuRw1uT30f5wvSyQoswDQYJKoZIhvcNAQELBQAwOTEYMBYGA1UEAwwPVEJPWC1VSUQtMDAwMDAxMRAwDgYDVQQKDAdPcGVuSU9WMQswCQYDVQQGEwJDTjAeFw0yNjA5MjgxMjE2MDJaFw0zNjA5MjUxMjE2MDJaMDkxGDAWBgNVBAMMD1RCT1gtVUlELTAwMDAwMTEQMA4GA1UECgwHT3BlbklPVjELMAkGA1UEBhMCQ04wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDM45ZQtClVtKSp59VRJqlnJPN9y+rfqSKTndr7xEoCx0fp1KTwHChLc9gxI21ETShfTo3pUcnyjRNcQ0l1JCnyWwmoLNMAVULwiCUUw3twElgyT31ncu8aYB6YIY/2W+dsHZhRKLFw81w333tr0FgG6aT622YPxxQ/RuRSedT674W79XLTanjpXGd+NoiCaUEo4bOxntpu1/uJ+Vvitu/kL0G/LPfFbnX2GMAPHz0U2Z8JOOgVh9/Vv9JfpIg+qMAciZaxfUWCuxneNoeRtdfe7kFFtc3f+A4pU1E5menYNqrky0lEleNzKHbdk+uE8qbH8Vv+BSdFjUW49d2a3B7NAgMBAAGjUzBRMB0GA1UdDgQWBBRcouDVdhUOHdlmtjq5qXSWmFn/pDAfBgNVHSMEGDAWgBRcouDVdhUOHdlmtjq5qXSWmFn/pDAPBgNVHRMBAf8EBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQBpwavwvsJaRQjD/iGqUDNjuYYpH17Q8yYwejHF/BNfM8B+z86k+7ly503VZylYJ84c0Kt9IXtJBAXpeQm5sVEhXybJ0VUPYRF9NMH3FIlWwgbIzG0YNLEYXa4KPsinixVjTiNFxRoxxO/jgfAFT2cmmmsqGrKz0ubwaztdZuf2jsiehABHebGs1yoeFHxJaDUTWvZTQ5tFLSUohjoo4gFa93Z4ICsSKfU9Bzc40GvbI0V/d65icM1Ose2Lhw3RqHJQbKrerdhWLCqRaQpHBApVad+1PG9VpBt39l3dwT1DQ7t9dwv197Jip6aZMnDEDQP2kTGdlFYnYiWK8p05zPe0";

    @MockBean
    private CertEnrollmentTemplate certificateEnrollmentTemplate;

    @Autowired
    private CertificateProvisioningAppService certificateProvisioningAppService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM tb_veh_certificate WHERE request_id = '" + REQUEST_ID + "'");
        jdbcTemplate.execute("DELETE FROM tb_vehicle_part WHERE vin = '" + VIN + "'");
        jdbcTemplate.execute("DELETE FROM tb_part_info WHERE sn = '" + DEVICE_SN + "'");
        jdbcTemplate.execute("DELETE FROM tb_veh_basic_info WHERE vin = '" + VIN + "'");

        jdbcTemplate.update(
                "INSERT INTO tb_veh_basic_info (vin, brand_code, platform_code, car_line_code, model_code, eol_time) " +
                        "VALUES (?, 'BRAND', 'PLAT', 'SER', 'MODEL', NOW())",
                VIN);
        jdbcTemplate.update(
                "INSERT INTO tb_part_info (part_code, sn, instance_state) VALUES (?, ?, 1)",
                PART_CODE, DEVICE_SN);
        jdbcTemplate.update(
                "INSERT INTO tb_vehicle_part (vin, part_id, bind_state) SELECT ?, id, 1 FROM tb_part_info WHERE sn = ?",
                VIN, DEVICE_SN);
    }

    @Test
    @DisplayName("产线证书申请：同步签发成功后响应应携带证书DER/链并登记subject/issuer")
    void applyDeviceCertificate_应返回证书DER并登记签发信息() {
        // Given
        byte[] leafDer = Base64.getDecoder().decode(CERT_DER_BASE64);
        when(certificateEnrollmentTemplate.apply(any())).thenReturn(
                new CertApplyResult(PKI_REQUEST_ID, EnrollmentState.ISSUED, Instant.now()));
        when(certificateEnrollmentTemplate.getCertificate(PKI_REQUEST_ID)).thenReturn(
                new IssuedCertificate(leafDer, List.of(leafDer), CERT_SN,
                        Instant.now(), Instant.now().plusSeconds(365L * 24 * 3600), "SHA256:FINGERPRINT"));

        CertificateApplyCmd cmd = CertificateApplyCmd.builder()
                .requestId(REQUEST_ID)
                .vin(VIN)
                .deviceCategory("TBOX")
                .deviceSn(DEVICE_SN)
                .certificateProfile("TBOX_TSP_CLIENT")
                .csrDerBase64(Base64.getEncoder().encodeToString(DEVICE_SN.getBytes()))
                .sourceSystem("MES")
                .facilityNo("FA-01")
                .lineCode("LINE-A")
                .build();

        // When
        CertificateApplyResult result = certificateProvisioningAppService.applyDeviceCertificate(cmd);

        // Then
        assertNotNull(result);
        assertEquals("ISSUED_NOT_CONFIRMED", result.getStatus());
        // 证书本体必须随响应回传（0x31 FF03 写入内容来源）
        assertEquals(CERT_DER_BASE64, result.getCertificateDerBase64());
        assertNotNull(result.getChainDerBase64());
        assertEquals(CERT_DER_BASE64, result.getChainDerBase64()[0]);
        assertEquals(CERT_SN, result.getCertSn());
        assertEquals("SHA256:FINGERPRINT", result.getFingerprint());

        // 落库登记：cert_sn / pki_request_id / subject / issuer（解析自证书 DER）
        // Java X500Principal.getName() 为 RFC2253 逆序：C=CN,O=OpenIOV,CN=TBOX-UID-000001
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_veh_certificate WHERE request_id = ? AND pki_request_id = ? AND cert_sn = ? " +
                        "AND cert_status = 'ISSUED_NOT_CONFIRMED' AND subject IS NOT NULL AND issuer IS NOT NULL",
                Integer.class, REQUEST_ID, PKI_REQUEST_ID, CERT_SN);
        assertEquals(1, count);
        String dbSubject = jdbcTemplate.queryForObject(
                "SELECT subject FROM tb_veh_certificate WHERE request_id = ?", String.class, REQUEST_ID);
        assertEquals("C=CN,O=OpenIOV,CN=TBOX-UID-000001", dbSubject);
    }

    @Test
    @DisplayName("证书状态查询：已签发后经pki_request_id重取回填证书本体")
    void queryCertificateStatus_应经PKI重取回填证书DER() {
        // Given：预置已签发记录（模拟 apply 已落库）
        jdbcTemplate.update(
                "INSERT INTO tb_veh_certificate (request_id, pki_request_id, cert_sn, vin, binding_id, part_id, " +
                        "device_category, device_sn, certificate_profile, cert_status) " +
                        "SELECT ?, ?, ?, ?, vp.id, pi.id, 'TBOX', ?, 'TBOX_TSP_CLIENT', 'ISSUED_NOT_CONFIRMED' " +
                        "FROM tb_vehicle_part vp JOIN tb_part_info pi ON pi.id = vp.part_id WHERE vp.vin = ? AND pi.sn = ?",
                REQUEST_ID, PKI_REQUEST_ID, CERT_SN, VIN, DEVICE_SN, VIN, DEVICE_SN);

        byte[] leafDer = Base64.getDecoder().decode(CERT_DER_BASE64);
        when(certificateEnrollmentTemplate.getCertificate(PKI_REQUEST_ID)).thenReturn(
                new IssuedCertificate(leafDer, List.of(leafDer), CERT_SN,
                        Instant.now(), Instant.now().plusSeconds(365L * 24 * 3600), "SHA256:FINGERPRINT"));

        // When
        CertificateStatusResult result = certificateProvisioningAppService.queryCertificateStatus(REQUEST_ID);

        // Then
        assertNotNull(result);
        assertEquals("ISSUED_NOT_CONFIRMED", result.getStatus());
        assertEquals(CERT_SN, result.getCertSn());
        assertEquals(CERT_DER_BASE64, result.getCertificateDerBase64());
        assertNotNull(result.getChainDerBase64());
        assertEquals(CERT_DER_BASE64, result.getChainDerBase64()[0]);
    }
}
