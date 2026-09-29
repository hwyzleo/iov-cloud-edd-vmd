package net.hwyz.iov.cloud.edd.vmd.service.integration;

import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.CertificateApplyCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.CertificateConfirmCmd;
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

import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
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
    /** 权威 HSM UID（与模拟 PKI 叶子证书 CN=TBOX-UID-000001 一致，CR-054） */
    private static final String HSM_UID = "TBOX-UID-000001";
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
        // BaseTest 仅 @Rollback 未开启测试事务（数据真实提交），清理必须覆盖全部变体以保证重跑幂等
        jdbcTemplate.execute("DELETE FROM tb_veh_certificate WHERE request_id LIKE 'CR044_REQ_001%'");
        jdbcTemplate.execute("DELETE FROM tb_vehicle_part WHERE vin = '" + VIN + "'");
        jdbcTemplate.execute("DELETE FROM tb_part_info WHERE sn = '" + DEVICE_SN + "'");
        jdbcTemplate.execute("DELETE FROM tb_veh_basic_info WHERE vin = '" + VIN + "'");

        jdbcTemplate.update(
                "INSERT INTO tb_veh_basic_info (vin, brand_code, platform_code, car_line_code, model_code, eol_time) " +
                        "VALUES (?, 'BRAND', 'PLAT', 'SER', 'MODEL', NOW())",
                VIN);
        jdbcTemplate.update(
                "INSERT INTO tb_part_info (part_code, sn, instance_state, extra) VALUES (?, ?, 1, ?)",
                PART_CODE, DEVICE_SN, "{\"hsm\":\"" + HSM_UID + "\"}");
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
                .csrDerBase64(buildValidCsrBase64(HSM_UID))
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

    @Test
    @DisplayName("换钥冲突：同身份同Profile不同公钥应拒绝签发（806064），旧证书不得被覆盖")
    void reissue_同身份不同公钥_应抛换钥冲突() {
        // Given：首次签发（CN=hsm_uid）成功并确认安装 → ACTIVE
        String req1 = REQUEST_ID + "_A";
        String pki1 = PKI_REQUEST_ID + "_A";
        String sn1 = CERT_SN + "-A";

        byte[] leafDer = Base64.getDecoder().decode(CERT_DER_BASE64);
        when(certificateEnrollmentTemplate.apply(any())).thenReturn(
                new CertApplyResult(pki1, EnrollmentState.ISSUED, Instant.now()));
        when(certificateEnrollmentTemplate.getCertificate(pki1)).thenReturn(
                new IssuedCertificate(leafDer, List.of(leafDer), sn1,
                        Instant.now(), Instant.now().plusSeconds(365L * 24 * 3600), "SHA256:FINGERPRINT"));

        CertificateApplyCmd cmd1 = CertificateApplyCmd.builder()
                .requestId(req1).vin(VIN).deviceCategory("TBOX").deviceSn(DEVICE_SN)
                .certificateProfile("TBOX_TSP_CLIENT").csrDerBase64(buildValidCsrBase64(HSM_UID))
                .sourceSystem("MES").build();
        certificateProvisioningAppService.applyDeviceCertificate(cmd1);
        certificateProvisioningAppService.confirmCertificateInstalled(CertificateConfirmCmd.builder()
                .requestId(req1).result("SUCCESS").vin(VIN).deviceSn(DEVICE_SN).build());

        // When：同身份（CN=hsm_uid）但新密钥（不同 SPKI）再次申请
        String req2 = REQUEST_ID + "_B";
        CertificateApplyCmd cmd2 = CertificateApplyCmd.builder()
                .requestId(req2).vin(VIN).deviceCategory("TBOX").deviceSn(DEVICE_SN)
                .certificateProfile("TBOX_TSP_CLIENT").csrDerBase64(buildValidCsrBase64(HSM_UID))
                .sourceSystem("MES").build();

        // Then：806064，不得返回旧证书、不得调 PKI 覆盖；旧证书保持 ACTIVE
        net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateKeyConflictException ex =
                assertThrows(net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateKeyConflictException.class,
                        () -> certificateProvisioningAppService.applyDeviceCertificate(cmd2));
        assertEquals("806064", ex.getErrorCode().getCode());

        String status1 = jdbcTemplate.queryForObject(
                "SELECT cert_status FROM tb_veh_certificate WHERE request_id = ?", String.class, req1);
        assertEquals("ACTIVE", status1);
        Integer activeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_veh_certificate WHERE device_sn = ? AND certificate_profile = 'TBOX_TSP_CLIENT' AND cert_status = 'ACTIVE'",
                Integer.class, DEVICE_SN);
        assertEquals(1, activeCount);
    }

    /**
     * 生成真实 PKCS#10 自签名 CSR（ECDSA P-256，CN=指定设备身份），
     * 替代旧的“明文设备SN冒充CSR”测试桩（TBOX-SEC-DSN-CR-015 §9.1）
     */
    private static String buildValidCsrBase64(String cn) {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(new ECGenParameterSpec("secp256r1"));
            KeyPair kp = kpg.generateKeyPair();
            X500Name subject = new X500Name("CN=" + cn + ",OU=TBOX-TSP,O=OpenIOV,C=CN");
            PKCS10CertificationRequestBuilder builder = new PKCS10CertificationRequestBuilder(
                    subject, SubjectPublicKeyInfo.getInstance(kp.getPublic().getEncoded()));
            ExtensionsGenerator extGen = new ExtensionsGenerator();
            extGen.addExtension(Extension.keyUsage, true,
                    new org.bouncycastle.asn1.x509.KeyUsage(
                            org.bouncycastle.asn1.x509.KeyUsage.digitalSignature));
            extGen.addExtension(Extension.extendedKeyUsage, false,
                    new org.bouncycastle.asn1.x509.ExtendedKeyUsage(
                            org.bouncycastle.asn1.x509.KeyPurposeId.id_kp_clientAuth));
            builder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extGen.generate());
            ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA").build(kp.getPrivate());
            PKCS10CertificationRequest req = builder.build(signer);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(req.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("生成测试CSR失败", e);
        }
    }
}
