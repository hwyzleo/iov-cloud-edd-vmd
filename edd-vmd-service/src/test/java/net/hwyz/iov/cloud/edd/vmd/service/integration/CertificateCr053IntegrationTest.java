package net.hwyz.iov.cloud.edd.vmd.service.integration;

import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.CompensateCertificateCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CertificateCompensateResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CertificateDetailResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.CertificateCompensationAppService;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateCompensationReasonRequiredException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateKeyConflictException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateRequestIdempotencyConflictException;
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
 * VMD-DSN-CR-053 集成测试
 * <p>
 * 覆盖：MPT 证书人工补申请、幂等/防重、对账、安装结果补录与操作审计的端到端链路。
 * 复用 CR-044 证书签发编排（真实 CertificateProvisioningAppService），仅 Mock framework PKI。
 * </p>
 *
 * @author CR-053
 */
@Rollback
class CertificateCr053IntegrationTest extends BaseTest {

    private static final String VIN = "HWYZTESTCR053001";
    private static final String DEVICE_SN = "TBOX-UID-CR053-001";
    /** 权威 HSM UID（与模拟 PKI 叶子证书 CN=TBOX-UID-000001 一致，CR-054） */
    private static final String HSM_UID = "TBOX-UID-000001";
    private static final String PART_CODE = "CR053_TBOX";
    private static final String PKI_REQUEST_ID = "PKI-CR053-001";
    private static final String CERT_SN = "CR053-CERT-001";

    private static final String CERT_DER_BASE64 =
            "MIIDUzCCAjugAwIBAgIUBVmSTuBfOQuRw1uT30f5wvSyQoswDQYJKoZIhvcNAQELBQAwOTEYMBYGA1UEAwwPVEJPWC1VSUQtMDAwMDAxMRAwDgYDVQQKDAdPcGVuSU9WMQswCQYDVQQGEwJDTjAeFw0yNjA5MjgxMjE2MDJaFw0zNjA5MjUxMjE2MDJaMDkxGDAWBgNVBAMMD1RCT1gtVUlELTAwMDAwMTEQMA4GA1UECgwHT3BlbklPVjELMAkGA1UEBhMCQ04wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDM45ZQtClVtKSp59VRJqlnJPN9y+rfqSKTndr7xEoCx0fp1KTwHChLc9gxI21ETShfTo3pUcnyjRNcQ0l1JCnyWwmoLNMAVULwiCUUw3twElgyT31ncu8aYB6YIY/2W+dsHZhRKLFw81w333tr0FgG6aT622YPxxQ/RuRSedT674W79XLTanjpXGd+NoiCaUEo4bOxntpu1/uJ+Vvitu/kL0G/LPfFbnX2GMAPHz0U2Z8JOOgVh9/Vv9JfpIg+qMAciZaxfUWCuxneNoeRtdfe7kFFtc3f+A4pU1E5menYNqrky0lEleNzKHbdk+uE8qbH8Vv+BSdFjUW49d2a3B7NAgMBAAGjUzBRMB0GA1UdDgQWBBRcouDVdhUOHdlmtjq5qXSWmFn/pDAfBgNVHSMEGDAWgBRcouDVdhUOHdlmtjq5qXSWmFn/pDAPBgNVHRMBAf8EBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQBpwavwvsJaRQjD/iGqUDNjuYYpH17Q8yYwejHF/BNfM8B+z86k+7ly503VZylYJ84c0Kt9IXtJBAXpeQm5sVEhXybJ0VUPYRF9NMH3FIlWwgbIzG0YNLEYXa4KPsinixVjTiNFxRoxxO/jgfAFT2cmmmsqGrKz0ubwaztdZuf2jsiehABHebGs1yoeFHxJaDUTWvZTQ5tFLSUohjoo4gFa93Z4ICsSKfU9Bzc40GvbI0V/d65icM1Ose2Lhw3RqHJQbKrerdhWLCqRaQpHBApVad+1PG9VpBt39l3dwT1DQ7t9dwv197Jip6aZMnDEDQP2kTGdlFYnYiWK8p05zPe0";

    @MockBean
    private CertEnrollmentTemplate certificateEnrollmentTemplate;

    @Autowired
    private CertificateCompensationAppService certificateCompensationAppService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // BaseTest 仅 @Rollback 未开启测试事务（数据真实提交），清理必须覆盖全部变体以保证重跑幂等
        jdbcTemplate.execute("DELETE FROM tb_veh_certificate_operation WHERE request_id IN " +
                "('MES-REQ-053','MES-REQ-053-CF','MES-REQ-053-MIS','MES-REQ-053-REC') OR request_id LIKE 'MPT-CERT-20260929%'");
        jdbcTemplate.execute("DELETE FROM tb_veh_certificate WHERE vin = '" + VIN + "' OR request_id IN " +
                "('MES-REQ-053','MES-REQ-053-CF','MES-REQ-053-MIS','MES-REQ-053-REC')");
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
    @DisplayName("人工补申请：合法请求应走既有签发编排并标记MPT_COMPENSATION来源与审计")
    void compensate_合法请求_应签发并审计() {
        // Given
        when(certificateEnrollmentTemplate.apply(any())).thenReturn(
                new CertApplyResult(PKI_REQUEST_ID, EnrollmentState.ISSUED, Instant.now()));
        when(certificateEnrollmentTemplate.getCertificate(PKI_REQUEST_ID)).thenReturn(
                new IssuedCertificate(Base64.getDecoder().decode(CERT_DER_BASE64),
                        List.of(Base64.getDecoder().decode(CERT_DER_BASE64)), CERT_SN,
                        Instant.now(), Instant.now().plusSeconds(365L * 24 * 3600), "SHA256:FINGERPRINT"));

        CompensateCertificateCmd cmd = CompensateCertificateCmd.builder()
                .vin(VIN).deviceCategory("TBOX").deviceSn(DEVICE_SN)
                .certificateProfile("TBOX_TSP_CLIENT").csrDerBase64(buildValidCsrBase64(HSM_UID))
                .originalMesRequestId("MES-ORIG-053").ticketNo("TICKET-053")
                .reason("MES请求未达，人工补申请")
                .operatorId("OP-001").operatorName("张三").sourceIp("10.1.1.1").userAgent("UA")
                .build();

        // When
        CertificateCompensateResult result = certificateCompensationAppService.compensate(cmd);

        // Then
        assertNotNull(result);
        assertTrue(result.getRequestId().startsWith("MPT-CERT-"));
        assertEquals("MPT_COMPENSATION", result.getSourceSystem());
        assertEquals(Boolean.FALSE, result.getReusedExisting());
        assertEquals("ISSUED_NOT_CONFIRMED", result.getStatus());

        // 落库：来源、原请求号、补偿原因、审计
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_veh_certificate WHERE request_id = ? AND source_system = 'MPT_COMPENSATION' " +
                        "AND original_request_id = 'MES-ORIG-053' AND compensation_reason = 'MES请求未达，人工补申请' " +
                        "AND ticket_no = 'TICKET-053' AND last_operator = 'OP-001' AND cert_status = 'ISSUED_NOT_CONFIRMED'",
                Integer.class, result.getRequestId());
        assertEquals(1, count);
        Integer opCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_veh_certificate_operation WHERE request_id = ? AND action = 'COMPENSATE' " +
                        "AND operator_name = '张三' AND reason = 'MES请求未达，人工补申请' AND result = 'SUCCESS' " +
                        "AND source_ip = '10.1.1.1' AND after_status = 'ISSUED_NOT_CONFIRMED'",
                Integer.class, result.getRequestId());
        assertEquals(1, opCount);
        // 审计不得保存 CSR 全文
        String digest = jdbcTemplate.queryForObject(
                "SELECT request_digest FROM tb_veh_certificate_operation WHERE request_id = ? AND action = 'COMPENSATE'",
                String.class, result.getRequestId());
        assertFalse(digest.contains(cmd.getCsrDerBase64()), "审计不得包含CSR全文");
    }

    @Test
    @DisplayName("人工补申请：requestId已存在同摘要应幂等返回（reusedExisting=true）")
    void compensate_requestId已存在同摘要_应幂等返回() {
        // Given：已存在同摘要申请（同 CSR）
        when(certificateEnrollmentTemplate.apply(any())).thenReturn(
                new CertApplyResult(PKI_REQUEST_ID, EnrollmentState.ISSUED, Instant.now()));
        when(certificateEnrollmentTemplate.getCertificate(PKI_REQUEST_ID)).thenReturn(
                new IssuedCertificate(Base64.getDecoder().decode(CERT_DER_BASE64),
                        List.of(Base64.getDecoder().decode(CERT_DER_BASE64)), CERT_SN,
                        Instant.now(), Instant.now().plusSeconds(365L * 24 * 3600), "SHA256:FINGERPRINT"));

        String csr = buildValidCsrBase64(HSM_UID);
        CertificateCompensateResult first = certificateCompensationAppService.compensate(
                CompensateCertificateCmd.builder()
                        .requestId("MES-REQ-053").vin(VIN).deviceCategory("TBOX").deviceSn(DEVICE_SN)
                        .certificateProfile("TBOX_TSP_CLIENT").csrDerBase64(csr)
                        .reason("首次补偿").operatorId("OP-001").operatorName("张三").build());

        // When：同 requestId 同摘要再次提交
        CertificateCompensateResult second = certificateCompensationAppService.compensate(
                CompensateCertificateCmd.builder()
                        .requestId("MES-REQ-053").vin(VIN).deviceCategory("TBOX").deviceSn(DEVICE_SN)
                        .certificateProfile("TBOX_TSP_CLIENT").csrDerBase64(csr)
                        .reason("重复提交").operatorId("OP-001").operatorName("张三").build());

        // Then：幂等返回既有记录，不重复签发
        assertEquals(first.getRequestId(), second.getRequestId());
        assertEquals(Boolean.TRUE, second.getReusedExisting());
        // 同 requestId 仅一条记录
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_veh_certificate WHERE request_id = 'MES-REQ-053'", Integer.class);
        assertEquals(1, count);
    }

    @Test
    @DisplayName("人工补申请：requestId已存在但摘要不一致应抛幂等冲突（806061）")
    void compensate_requestId已存在异摘要_应抛幂等冲突() {
        // Given：已存在申请（CSR-A）
        when(certificateEnrollmentTemplate.apply(any())).thenReturn(
                new CertApplyResult(PKI_REQUEST_ID, EnrollmentState.ISSUED, Instant.now()));
        when(certificateEnrollmentTemplate.getCertificate(PKI_REQUEST_ID)).thenReturn(
                new IssuedCertificate(Base64.getDecoder().decode(CERT_DER_BASE64),
                        List.of(Base64.getDecoder().decode(CERT_DER_BASE64)), CERT_SN,
                        Instant.now(), Instant.now().plusSeconds(365L * 24 * 3600), "SHA256:FINGERPRINT"));
        certificateCompensationAppService.compensate(
                CompensateCertificateCmd.builder()
                        .requestId("MES-REQ-053").vin(VIN).deviceCategory("TBOX").deviceSn(DEVICE_SN)
                        .certificateProfile("TBOX_TSP_CLIENT").csrDerBase64(buildValidCsrBase64(HSM_UID))
                        .reason("首次补偿").operatorId("OP-001").operatorName("张三").build());

        // When：同 requestId 但不同 CSR（新密钥 → 不同指纹）
        String differentCsr = buildValidCsrBase64(HSM_UID);

        // Then：摘要不一致拒绝
        assertThrows(CertificateRequestIdempotencyConflictException.class,
                () -> certificateCompensationAppService.compensate(
                        CompensateCertificateCmd.builder()
                                .requestId("MES-REQ-053").vin(VIN).deviceCategory("TBOX").deviceSn(DEVICE_SN)
                                .certificateProfile("TBOX_TSP_CLIENT").csrDerBase64(differentCsr)
                                .reason("重试").operatorId("OP-001").operatorName("张三").build()));
    }

    @Test
    @DisplayName("人工补申请：同身份不同公钥已有ACTIVE应抛换钥冲突（806064）")
    void compensate_已有ACTIVE异SPKI_应抛换钥冲突() {
        // Given：首次申请成功并安装确认 → ACTIVE
        when(certificateEnrollmentTemplate.apply(any())).thenReturn(
                new CertApplyResult(PKI_REQUEST_ID, EnrollmentState.ISSUED, Instant.now()));
        when(certificateEnrollmentTemplate.getCertificate(PKI_REQUEST_ID)).thenReturn(
                new IssuedCertificate(Base64.getDecoder().decode(CERT_DER_BASE64),
                        List.of(Base64.getDecoder().decode(CERT_DER_BASE64)), CERT_SN,
                        Instant.now(), Instant.now().plusSeconds(365L * 24 * 3600), "SHA256:FINGERPRINT"));
        CertificateCompensateResult first = certificateCompensationAppService.compensate(
                CompensateCertificateCmd.builder()
                        .vin(VIN).deviceCategory("TBOX").deviceSn(DEVICE_SN)
                        .certificateProfile("TBOX_TSP_CLIENT").csrDerBase64(buildValidCsrBase64(HSM_UID))
                        .reason("首次补偿").operatorId("OP-001").operatorName("张三").build());
        certificateCompensationAppService.confirmInstalled(
                jdbcTemplate.queryForObject("SELECT id FROM tb_veh_certificate WHERE request_id = ?", Long.class, first.getRequestId()),
                CERT_SN, DEVICE_SN, "SUCCESS", null, "工位确认", "TICKET-053",
                "OP-001", "张三", "10.1.1.1", "UA");

        // When：同身份（CN=hsm_uid）但不同公钥（新密钥）再补申请
        String differentCsr = buildValidCsrBase64(HSM_UID);
        // Then：806064 换钥冲突，普通补偿拒绝，不得返回旧证书
        net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateKeyConflictException ex =
                assertThrows(net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateKeyConflictException.class,
                        () -> certificateCompensationAppService.compensate(
                                CompensateCertificateCmd.builder()
                                        .vin(VIN).deviceCategory("TBOX").deviceSn(DEVICE_SN)
                                        .certificateProfile("TBOX_TSP_CLIENT").csrDerBase64(differentCsr)
                                        .reason("重签").operatorId("OP-001").operatorName("张三").build()));
        assertEquals("806064", ex.getErrorCode().getCode());
    }

    @Test
    @DisplayName("对账：有pki_request_id且PKI已签发应推进至ISSUED_NOT_CONFIRMED")
    void reconcile_有pki且PKI已签发_应推进() {
        // Given：预置 ISSUING + pki_request_id 记录
        jdbcTemplate.update(
                "INSERT INTO tb_veh_certificate (request_id, pki_request_id, vin, binding_id, part_id, " +
                        "device_category, device_sn, certificate_profile, cert_status) " +
                        "SELECT 'MES-REQ-053-REC', ?, ?, vp.id, pi.id, 'TBOX', ?, 'TBOX_TSP_CLIENT', 'ISSUING' " +
                        "FROM tb_vehicle_part vp JOIN tb_part_info pi ON pi.id = vp.part_id " +
                        "WHERE vp.vin = ? AND pi.sn = ?",
                PKI_REQUEST_ID, VIN, DEVICE_SN, VIN, DEVICE_SN);
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_veh_certificate WHERE request_id = 'MES-REQ-053-REC'", Long.class);
        when(certificateEnrollmentTemplate.getStatus(PKI_REQUEST_ID)).thenReturn(
                new CertApplyResult(PKI_REQUEST_ID, EnrollmentState.ISSUED, Instant.now()));
        when(certificateEnrollmentTemplate.getCertificate(PKI_REQUEST_ID)).thenReturn(
                new IssuedCertificate(Base64.getDecoder().decode(CERT_DER_BASE64),
                        List.of(Base64.getDecoder().decode(CERT_DER_BASE64)), CERT_SN,
                        Instant.now(), Instant.now().plusSeconds(365L * 24 * 3600), "SHA256:FINGERPRINT"));

        // When
        CertificateCompensateResult result = certificateCompensationAppService.reconcile(
                id, "PKI超时对账", "OP-001", "张三", "10.1.1.1", "UA");

        // Then
        assertEquals("ISSUED_NOT_CONFIRMED", result.getStatus());
        Integer opCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_veh_certificate_operation WHERE request_id = 'MES-REQ-053-REC' " +
                        "AND action = 'RECONCILE' AND before_status = 'ISSUING' AND after_status = 'ISSUED_NOT_CONFIRMED'",
                Integer.class);
        assertEquals(1, opCount);
    }

    @Test
    @DisplayName("人工补申请：首次PKI失败转FAILED后用同一CSR重试应按原键重新签发（失败重试）")
    void compensate_FAILED后用同一CSR重试_应按原键重新签发() {
        // Given：首次补偿时 PKI 异常 → 记录置 FAILED
        when(certificateEnrollmentTemplate.apply(any()))
                .thenThrow(new RuntimeException("PKI临时不可用"));
        String csr = buildValidCsrBase64(HSM_UID);
        try {
            certificateCompensationAppService.compensate(
                    CompensateCertificateCmd.builder()
                            .vin(VIN).deviceCategory("TBOX").deviceSn(DEVICE_SN)
                            .certificateProfile("TBOX_TSP_CLIENT").csrDerBase64(csr)
                            .reason("首次补偿").operatorId("OP-001").operatorName("张三").build());
            throw new AssertionError("首次补偿应因 PKI 异常失败");
        } catch (RuntimeException expected) {
            // PKI 异常传播，记录已落 FAILED
        }
        String firstRequestId = jdbcTemplate.queryForObject(
                "SELECT request_id FROM tb_veh_certificate WHERE vin = ? AND cert_status = 'FAILED' ORDER BY id DESC LIMIT 1",
                String.class, VIN);
        assertNotNull(firstRequestId);
        assertTrue(firstRequestId.startsWith("MPT-CERT-"));

        // When：PKI 恢复，同一 CSR 再次人工补申请（失败重试）
        when(certificateEnrollmentTemplate.apply(any())).thenReturn(
                new CertApplyResult(PKI_REQUEST_ID + "-RETRY", EnrollmentState.ISSUED, Instant.now()));
        when(certificateEnrollmentTemplate.getCertificate(PKI_REQUEST_ID + "-RETRY")).thenReturn(
                new IssuedCertificate(Base64.getDecoder().decode(CERT_DER_BASE64),
                        List.of(Base64.getDecoder().decode(CERT_DER_BASE64)), CERT_SN,
                        Instant.now(), Instant.now().plusSeconds(365L * 24 * 3600), "SHA256:FINGERPRINT"));
        CertificateCompensateResult retry = certificateCompensationAppService.compensate(
                CompensateCertificateCmd.builder()
                        .vin(VIN).deviceCategory("TBOX").deviceSn(DEVICE_SN)
                        .certificateProfile("TBOX_TSP_CLIENT").csrDerBase64(csr)
                        .reason("失败重试").operatorId("OP-001").operatorName("张三").build());

        // Then：按原 requestId 重新签发成功推进至 ISSUED_NOT_CONFIRMED，不新建申请
        assertEquals(firstRequestId, retry.getRequestId());
        assertEquals("ISSUED_NOT_CONFIRMED", retry.getStatus());
        assertEquals(Boolean.FALSE, retry.getReusedExisting());
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_veh_certificate WHERE request_id = ? AND cert_status = 'ISSUED_NOT_CONFIRMED'",
                Integer.class, firstRequestId);
        assertEquals(1, count);
        Integer retryOpCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_veh_certificate_operation WHERE request_id = ? AND action = 'COMPENSATE' " +
                        "AND before_status = 'FAILED' AND after_status = 'ISSUED_NOT_CONFIRMED' AND result = 'SUCCESS'",
                Integer.class, firstRequestId);
        assertEquals(1, retryOpCount);
    }

    @Test
    @DisplayName("安装补录：ISSUED_NOT_CONFIRMED 安装失败转 INSTALL_FAILED 后重试成功")
    void confirmInstalled_失败后重试成功() {
        // Given：预置 ISSUED_NOT_CONFIRMED 记录
        jdbcTemplate.update(
                "INSERT INTO tb_veh_certificate (request_id, pki_request_id, cert_sn, vin, binding_id, part_id, " +
                        "device_category, device_sn, certificate_profile, cert_status) " +
                        "SELECT 'MES-REQ-053-CF', ?, ?, ?, vp.id, pi.id, 'TBOX', ?, 'TBOX_TSP_CLIENT', 'ISSUED_NOT_CONFIRMED' " +
                        "FROM tb_vehicle_part vp JOIN tb_part_info pi ON pi.id = vp.part_id " +
                        "WHERE vp.vin = ? AND pi.sn = ?",
                PKI_REQUEST_ID, CERT_SN, VIN, DEVICE_SN, VIN, DEVICE_SN);
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_veh_certificate WHERE request_id = 'MES-REQ-053-CF'", Long.class);

        // When：首次安装失败
        certificateCompensationAppService.confirmInstalled(
                id, CERT_SN, DEVICE_SN, "FAILED", "安装超时", "安装失败", "TICKET-053",
                "OP-001", "张三", "10.1.1.1", "UA");
        String failed = jdbcTemplate.queryForObject(
                "SELECT cert_status FROM tb_veh_certificate WHERE request_id = 'MES-REQ-053-CF'", String.class);
        assertEquals("INSTALL_FAILED", failed);

        // When：重新注入后重试成功（INSTALL_FAILED 允许重试，CR-053 共享内核）
        certificateCompensationAppService.confirmInstalled(
                id, CERT_SN, DEVICE_SN, "SUCCESS", null, "重新注入后确认", "TICKET-053",
                "OP-001", "张三", "10.1.1.1", "UA");
        String active = jdbcTemplate.queryForObject(
                "SELECT cert_status FROM tb_veh_certificate WHERE request_id = 'MES-REQ-053-CF'", String.class);
        assertEquals("ACTIVE", active);
    }

    @Test
    @DisplayName("安装补录：certSn不匹配应拒绝且不改变状态")
    void confirmInstalled_certSn不匹配_应拒绝() {
        // Given
        jdbcTemplate.update(
                "INSERT INTO tb_veh_certificate (request_id, pki_request_id, cert_sn, vin, binding_id, part_id, " +
                        "device_category, device_sn, certificate_profile, cert_status) " +
                        "SELECT 'MES-REQ-053-MIS', ?, ?, ?, vp.id, pi.id, 'TBOX', ?, 'TBOX_TSP_CLIENT', 'ISSUED_NOT_CONFIRMED' " +
                        "FROM tb_vehicle_part vp JOIN tb_part_info pi ON pi.id = vp.part_id " +
                        "WHERE vp.vin = ? AND pi.sn = ?",
                PKI_REQUEST_ID, CERT_SN, VIN, DEVICE_SN, VIN, DEVICE_SN);
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_veh_certificate WHERE request_id = 'MES-REQ-053-MIS'", Long.class);

        // When/Then：certSn 不匹配拒绝
        assertThrows(IllegalStateException.class,
                () -> certificateCompensationAppService.confirmInstalled(
                        id, "CERT-WRONG", DEVICE_SN, "SUCCESS", null, "补录", "TICKET-053",
                        "OP-001", "张三", "10.1.1.1", "UA"));
        String status = jdbcTemplate.queryForObject(
                "SELECT cert_status FROM tb_veh_certificate WHERE request_id = 'MES-REQ-053-MIS'", String.class);
        assertEquals("ISSUED_NOT_CONFIRMED", status);
    }

    @Test
    @DisplayName("查询：详情仅返回CSR指纹，不返回CSR全文，含脱敏操作时间线")
    void getDetail_应脱敏且含操作时间线() {
        // Given：签发一条申请
        when(certificateEnrollmentTemplate.apply(any())).thenReturn(
                new CertApplyResult(PKI_REQUEST_ID, EnrollmentState.ISSUED, Instant.now()));
        when(certificateEnrollmentTemplate.getCertificate(PKI_REQUEST_ID)).thenReturn(
                new IssuedCertificate(Base64.getDecoder().decode(CERT_DER_BASE64),
                        List.of(Base64.getDecoder().decode(CERT_DER_BASE64)), CERT_SN,
                        Instant.now(), Instant.now().plusSeconds(365L * 24 * 3600), "SHA256:FINGERPRINT"));
        CertificateCompensateResult comp = certificateCompensationAppService.compensate(
                CompensateCertificateCmd.builder()
                        .vin(VIN).deviceCategory("TBOX").deviceSn(DEVICE_SN)
                        .certificateProfile("TBOX_TSP_CLIENT").csrDerBase64(buildValidCsrBase64(HSM_UID))
                        .reason("查询脱敏验证").operatorId("OP-001").operatorName("张三").build());
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_veh_certificate WHERE request_id = ?", Long.class, comp.getRequestId());

        // When
        CertificateDetailResult detail = certificateCompensationAppService.getDetail(id);

        // Then
        assertEquals("MPT_COMPENSATION", detail.getSourceSystem());
        assertNotNull(detail.getCsrFingerprint());
        // 详情不暴露 CSR 全文（仅指纹）
        assertFalse(detail.toString().contains("BEGIN CERTIFICATE REQUEST"));
        assertTrue(detail.getOperations().stream().anyMatch(op -> "COMPENSATE".equals(op.getAction())));
    }

    @Test
    @DisplayName("人工补申请：缺reason应拒绝（806063）且不写审计")
    void compensate_缺reason_应拒绝() {
        assertThrows(CertificateCompensationReasonRequiredException.class,
                () -> certificateCompensationAppService.compensate(
                        CompensateCertificateCmd.builder()
                                .vin(VIN).deviceCategory("TBOX").deviceSn(DEVICE_SN)
                                .certificateProfile("TBOX_TSP_CLIENT").csrDerBase64(buildValidCsrBase64(HSM_UID))
                                .operatorId("OP-001").operatorName("张三").build()));
        Integer opCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_veh_certificate WHERE vin = '" + VIN + "'", Integer.class);
        assertEquals(0, opCount);
    }

    /**
     * 生成真实 PKCS#10 自签名 CSR（ECDSA P-256，CN=指定设备身份）
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
