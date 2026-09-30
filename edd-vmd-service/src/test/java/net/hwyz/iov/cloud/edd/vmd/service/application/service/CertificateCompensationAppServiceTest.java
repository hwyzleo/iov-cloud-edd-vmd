package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.CompensateCertificateCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CertificateApplyResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CertificateCompensateResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CertificateDetailResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.query.VehicleCertificateQuery;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateCompensationReasonRequiredException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateIssuanceConflictException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateKeyConflictException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateRequestIdempotencyConflictException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleCertificate;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleCertificateOperation;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.CertificateStatus;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleCertificateOperationRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleCertificateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * CertificateCompensationAppService 单元测试（VMD-DSN-CR-053 / CR-054）
 * <p>
 * CR-054：补偿与 OAPI 共用身份解析/CN/SPKI 门禁，业务去重切至 vin+hsm_uid+public_key_sha256+profile；
 * 同身份不同公钥返回换钥冲突（806064），MPT 不要求管理员手填 UID。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
class CertificateCompensationAppServiceTest {

    private static final String VIN = "HWYZTESTCR053001";
    private static final String DEVICE_SN = "00000005AA00000001";
    private static final String HSM_UID = "00000000000000000000000000000001";
    private static final String PROFILE = "TBOX_TSP_CLIENT";
    private static final String SPKI = "SPKI_SHA256_MOCK";

    @Mock
    private CertificateProvisioningAppService certificateProvisioningAppService;

    @Mock
    private VehicleCertificateRepository vehicleCertificateRepository;

    @Mock
    private VehicleCertificateOperationRepository vehicleCertificateOperationRepository;

    @Mock
    private BoundDeviceIdentityResolver boundDeviceIdentityResolver;

    @Mock
    private CertificateIdentityValidator certificateIdentityValidator;

    @InjectMocks
    private CertificateCompensationAppService certificateCompensationAppService;

    private String csrBase64;

    @BeforeEach
    void setUp() {
        csrBase64 = buildValidCsrBase64(HSM_UID);
        BoundDeviceIdentity identity = new BoundDeviceIdentity(VIN, 10L, 1L, DEVICE_SN, "TBOX", HSM_UID, "BOTH");
        lenient().when(boundDeviceIdentityResolver.resolve(VIN, DEVICE_SN, "TBOX")).thenReturn(identity);
        lenient().when(certificateIdentityValidator.validate(any(), any(), any(), any()))
                .thenReturn(new ParsedCsr(HSM_UID, SPKI, fingerprintOf(csrBase64)));
    }

    // ---------- compensate：人工补申请 ----------

    @Test
    @DisplayName("人工补申请：合法请求应委托内核并写MPT_COMPENSATION来源")
    void compensate_合法请求_应委托内核并标记MPT来源() {
        // Given
        VehicleCertificate saved = cert(CertificateStatus.ISSUED_NOT_CONFIRMED, null);
        saved.setSourceSystem("MPT_COMPENSATION");
        when(vehicleCertificateRepository.selectByRequestId(any())).thenReturn(saved);
        when(certificateProvisioningAppService.applyDeviceCertificate(any())).thenReturn(
                CertificateApplyResult.builder().requestId("MPT-CERT-20260929-XXXXXXXX").status("ISSUED_NOT_CONFIRMED").build());

        CompensateCertificateCmd cmd = baseCmd().requestId(null).originalMesRequestId("MES-REQ-001").reason("MES请求未达，人工补申请").build();

        // When
        CertificateCompensateResult result = certificateCompensationAppService.compensate(cmd);

        // Then
        assertNotNull(result);
        assertEquals("MPT_COMPENSATION", result.getSourceSystem());
        assertEquals(Boolean.FALSE, result.getReusedExisting());
        assertEquals("QUERY", result.getNextAction());
        // 委托内核且 requestId 由服务端生成（MPT-CERT 前缀）
        ArgumentCaptor<net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.CertificateApplyCmd> captor =
                ArgumentCaptor.forClass(net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.CertificateApplyCmd.class);
        verify(certificateProvisioningAppService).applyDeviceCertificate(captor.capture());
        assertTrue(captor.getValue().getRequestId().startsWith("MPT-CERT-"));
        assertEquals("MPT_COMPENSATION", captor.getValue().getSourceSystem());
        assertEquals("MES-REQ-001", captor.getValue().getOriginalRequestId());
        assertEquals("MES请求未达，人工补申请", captor.getValue().getCompensationReason());
        // MPT 不要求管理员手填 UID：declaredEcuUid 为空（CR-054 RD-054-3）
        assertNull(captor.getValue().getDeclaredEcuUid());
        // 审计落库（操作人/原因/请求摘要，无CSR全文）
        ArgumentCaptor<VehicleCertificateOperation> opCaptor = ArgumentCaptor.forClass(VehicleCertificateOperation.class);
        verify(vehicleCertificateOperationRepository).insert(opCaptor.capture());
        VehicleCertificateOperation op = opCaptor.getValue();
        assertEquals("COMPENSATE", op.getAction());
        assertEquals("OP-001", op.getOperatorId());
        assertEquals("MES请求未达，人工补申请", op.getReason());
        assertTrue(op.getRequestDigest().contains("csrFingerprint="));
        assertFalse(op.getRequestDigest().contains(csrBase64), "审计不得包含CSR全文");
    }

    @Test
    @DisplayName("人工补申请：缺reason应拒绝（806063）")
    void compensate_缺reason_应拒绝() {
        CompensateCertificateCmd cmd = baseCmd().reason(null).build();
        assertThrows(CertificateCompensationReasonRequiredException.class,
                () -> certificateCompensationAppService.compensate(cmd));
        verify(certificateProvisioningAppService, never()).applyDeviceCertificate(any());
    }

    // ---------- compensate：幂等 ----------

    @Test
    @DisplayName("人工补申请：requestId已存在且摘要一致应幂等返回既有状态")
    void compensate_requestId已存在同摘要_应幂等返回() {
        // Given
        VehicleCertificate existing = cert(CertificateStatus.ACTIVE, "MES-REQ-EXIST");
        existing.setCsrFingerprint(fingerprintOf(csrBase64));
        when(vehicleCertificateRepository.selectByRequestId("MES-REQ-EXIST")).thenReturn(existing);

        CompensateCertificateCmd cmd = baseCmd().requestId("MES-REQ-EXIST").reason("重复提交").build();

        // When
        CertificateCompensateResult result = certificateCompensationAppService.compensate(cmd);

        // Then
        assertEquals(Boolean.TRUE, result.getReusedExisting());
        assertEquals("ACTIVE", result.getStatus());
        assertEquals("QUERY", result.getNextAction());
        verify(certificateProvisioningAppService, never()).applyDeviceCertificate(any());
    }

    @Test
    @DisplayName("人工补申请：requestId已存在但摘要不一致应抛幂等冲突（806061）")
    void compensate_requestId已存在异摘要_应抛幂等冲突() {
        // Given：已有记录指纹与本次 CSR 不同
        VehicleCertificate existing = cert(CertificateStatus.REQUESTED, "MES-REQ-CONFLICT");
        existing.setCsrFingerprint("different-fingerprint");
        when(vehicleCertificateRepository.selectByRequestId("MES-REQ-CONFLICT")).thenReturn(existing);

        CompensateCertificateCmd cmd = baseCmd().requestId("MES-REQ-CONFLICT").reason("重试").build();

        // When/Then
        assertThrows(CertificateRequestIdempotencyConflictException.class,
                () -> certificateCompensationAppService.compensate(cmd));
        verify(certificateProvisioningAppService, never()).applyDeviceCertificate(any());
    }

    @Test
    @DisplayName("人工补申请：originalMesRequestId命中既有记录应引导reconcile，不创建新申请")
    void compensate_原请求号命中_应引导对账() {
        // Given
        VehicleCertificate existing = cert(CertificateStatus.PENDING_RECONCILE, "MES-REQ-ORIG");
        when(vehicleCertificateRepository.selectByOriginalRequestId("MES-ORIG-001")).thenReturn(existing);

        CompensateCertificateCmd cmd = baseCmd().requestId(null).originalMesRequestId("MES-ORIG-001").reason("补申请").build();

        // When
        CertificateCompensateResult result = certificateCompensationAppService.compensate(cmd);

        // Then
        assertEquals(Boolean.TRUE, result.getReusedExisting());
        assertEquals("RECONCILE", result.getNextAction());
        assertEquals("MES-REQ-ORIG", result.getRequestId());
        verify(certificateProvisioningAppService, never()).applyDeviceCertificate(any());
    }

    // ---------- compensate：业务防重（CR-054：身份键） ----------

    @Test
    @DisplayName("人工补申请：同身份同公钥已有ACTIVE且同摘要应幂等返回")
    void compensate_同业务键同摘要_应幂等返回() {
        // Given：同 (vin, hsm_uid, spki, profile) 已有 ISSUED_NOT_CONFIRMED
        VehicleCertificate existing = cert(CertificateStatus.ISSUED_NOT_CONFIRMED, "MES-REQ-SAME");
        existing.setHsmUid(HSM_UID);
        existing.setPublicKeySha256(SPKI);
        existing.setCsrFingerprint(fingerprintOf(csrBase64));
        when(vehicleCertificateRepository.selectByVinAndUidAndSpkiAndProfile(VIN, HSM_UID, SPKI, PROFILE))
                .thenReturn(existing);

        CompensateCertificateCmd cmd = baseCmd().build();

        // When
        CertificateCompensateResult result = certificateCompensationAppService.compensate(cmd);

        // Then
        assertEquals(Boolean.TRUE, result.getReusedExisting());
        assertEquals("QUERY", result.getNextAction());
        verify(certificateProvisioningAppService, never()).applyDeviceCertificate(any());
    }

    @Test
    @DisplayName("人工补申请：同业务键但摘要不一致应抛签发冲突（806062）")
    void compensate_同业务键异摘要_应抛签发冲突() {
        // Given：同业务键但请求摘要不一致（CSR 指纹相同但 VIN/设备等字段不同）
        VehicleCertificate active = cert(CertificateStatus.ACTIVE, "MES-REQ-ACTIVE");
        active.setHsmUid(HSM_UID);
        active.setPublicKeySha256(SPKI);
        active.setCsrFingerprint("different-fingerprint");
        when(vehicleCertificateRepository.selectByVinAndUidAndSpkiAndProfile(VIN, HSM_UID, SPKI, PROFILE))
                .thenReturn(active);

        CompensateCertificateCmd cmd = baseCmd().build();

        // When/Then：806062
        assertThrows(CertificateIssuanceConflictException.class,
                () -> certificateCompensationAppService.compensate(cmd));
        verify(certificateProvisioningAppService, never()).applyDeviceCertificate(any());
    }

    @Test
    @DisplayName("人工补申请：同身份不同公钥已有有效证书应抛换钥冲突（806064）")
    void compensate_同身份不同SPKI_应抛换钥冲突() {
        // Given：同 (vin, hsm_uid, profile) 已有不同 SPKI 的 ACTIVE
        VehicleCertificate otherKey = cert(CertificateStatus.ACTIVE, "MES-REQ-OTHER-KEY");
        otherKey.setHsmUid(HSM_UID);
        otherKey.setPublicKeySha256("SPKI_OTHER_KEY");
        when(vehicleCertificateRepository.selectByVinAndUidAndSpkiAndProfile(VIN, HSM_UID, SPKI, PROFILE))
                .thenReturn(null);
        when(vehicleCertificateRepository.selectKeyConflictByVinAndUidAndProfile(VIN, HSM_UID, SPKI, PROFILE))
                .thenReturn(otherKey);

        CompensateCertificateCmd cmd = baseCmd().build();

        // When/Then：806064，普通补偿拒绝，不得返回旧证书
        CertificateKeyConflictException ex = assertThrows(CertificateKeyConflictException.class,
                () -> certificateCompensationAppService.compensate(cmd));
        assertEquals("806064", ex.getErrorCode().getCode());
        verify(certificateProvisioningAppService, never()).applyDeviceCertificate(any());
    }

    @Test
    @DisplayName("人工补申请：同摘要REQUESTED无pki应按原键继续签发（不生成新键）")
    void compensate_同摘要REQUESTED无pki_应按原键继续签发() {
        // Given
        VehicleCertificate existing = cert(CertificateStatus.REQUESTED, "MES-REQ-CONT");
        existing.setHsmUid(HSM_UID);
        existing.setPublicKeySha256(SPKI);
        existing.setCsrFingerprint(fingerprintOf(csrBase64));
        existing.setPkiRequestId(null);
        when(vehicleCertificateRepository.selectByVinAndUidAndSpkiAndProfile(VIN, HSM_UID, SPKI, PROFILE))
                .thenReturn(existing);
        VehicleCertificate updated = cert(CertificateStatus.ISSUING, "MES-REQ-CONT");
        updated.setPkiRequestId("PKI-053-001");
        when(certificateProvisioningAppService.reconcile(eq("MES-REQ-CONT"), any(), any(), any()))
                .thenReturn(CertificateApplyResult.builder().requestId("MES-REQ-CONT").status("ISSUING").build());
        when(vehicleCertificateRepository.selectByRequestId("MES-REQ-CONT")).thenReturn(updated);

        CompensateCertificateCmd cmd = baseCmd().requestId(null).build();

        // When
        CertificateCompensateResult result = certificateCompensationAppService.compensate(cmd);

        // Then：按原键继续签发，未生成新 requestId
        verify(certificateProvisioningAppService).reconcile(eq("MES-REQ-CONT"), eq(csrBase64), eq("OP-001"), any());
        verify(certificateProvisioningAppService, never()).applyDeviceCertificate(any());
        assertEquals(Boolean.FALSE, result.getReusedExisting());
        assertEquals("RECONCILE", result.getNextAction());
    }

    @Test
    @DisplayName("人工补申请：同业务键同摘要FAILED应按原键重新签发（失败重试）而非幂等返回")
    void compensate_同业务键同摘要FAILED_应委托reconcile重发() {
        // Given：同 (vin, hsm_uid, spki, profile) 已有 FAILED 终态记录，本次提交同摘要 CSR（失败重试）
        VehicleCertificate existing = cert(CertificateStatus.FAILED, "MPT-CERT-20260929-FAILED");
        existing.setHsmUid(HSM_UID);
        existing.setPublicKeySha256(SPKI);
        existing.setCsrFingerprint(fingerprintOf(csrBase64));
        when(vehicleCertificateRepository.selectByVinAndUidAndSpkiAndProfile(VIN, HSM_UID, SPKI, PROFILE))
                .thenReturn(existing);
        VehicleCertificate updated = cert(CertificateStatus.ISSUING, "MPT-CERT-20260929-FAILED");
        updated.setPkiRequestId("PKI-053-RETRY");
        when(certificateProvisioningAppService.reconcile(eq("MPT-CERT-20260929-FAILED"), any(), any(), any()))
                .thenReturn(CertificateApplyResult.builder().requestId("MPT-CERT-20260929-FAILED").status("ISSUING").build());
        when(vehicleCertificateRepository.selectByRequestId("MPT-CERT-20260929-FAILED")).thenReturn(updated);

        CompensateCertificateCmd cmd = baseCmd().requestId(null).build();

        // When
        CertificateCompensateResult result = certificateCompensationAppService.compensate(cmd);

        // Then：FAILED 按原键重新签发，而非幂等返回 FAILED
        verify(certificateProvisioningAppService).reconcile(eq("MPT-CERT-20260929-FAILED"), eq(csrBase64), eq("OP-001"), any());
        verify(certificateProvisioningAppService, never()).applyDeviceCertificate(any());
        assertEquals(Boolean.FALSE, result.getReusedExisting());
        assertEquals("ISSUING", result.getStatus());
    }

    // ---------- reconcile ----------

    @Test
    @DisplayName("对账：委托内核并写审计（含前后状态）")
    void reconcile_应委托内核并写审计() {
        // Given
        VehicleCertificate cert = cert(CertificateStatus.ISSUING, "MES-REQ-REC");
        cert.setPkiRequestId("PKI-REC-001");
        when(vehicleCertificateRepository.selectById(1L)).thenReturn(cert);
        VehicleCertificate updated = cert(CertificateStatus.ISSUED_NOT_CONFIRMED, "MES-REQ-REC");
        updated.setPkiRequestId("PKI-REC-001");
        when(vehicleCertificateRepository.selectByRequestId("MES-REQ-REC")).thenReturn(updated);
        when(certificateProvisioningAppService.reconcile(eq("MES-REQ-REC"), any(), any(), any()))
                .thenReturn(CertificateApplyResult.builder().requestId("MES-REQ-REC").status("ISSUED_NOT_CONFIRMED").build());

        // When
        CertificateCompensateResult result = certificateCompensationAppService.reconcile(
                1L, "PKI超时对账", "OP-001", "张三", "10.1.1.1", "Mozilla/5.0");

        // Then
        assertEquals("ISSUED_NOT_CONFIRMED", result.getStatus());
        ArgumentCaptor<VehicleCertificateOperation> opCaptor = ArgumentCaptor.forClass(VehicleCertificateOperation.class);
        verify(vehicleCertificateOperationRepository).insert(opCaptor.capture());
        assertEquals("RECONCILE", opCaptor.getValue().getAction());
        assertEquals("ISSUING", opCaptor.getValue().getBeforeStatus());
        assertEquals("ISSUED_NOT_CONFIRMED", opCaptor.getValue().getAfterStatus());
        assertEquals("10.1.1.1", opCaptor.getValue().getSourceIp());
        assertEquals("Mozilla/5.0", opCaptor.getValue().getUserAgent());
    }

    // ---------- queryCertificate（只读获取证书本体） ----------

    @Test
    @DisplayName("获取证书本体：已签发未确认应复用只读查询回填本体并写QUERY审计")
    void queryCertificate_已签发_应回填本体() {
        // Given
        VehicleCertificate cert = cert(CertificateStatus.ISSUED_NOT_CONFIRMED, "MES-REQ-Q1");
        cert.setPkiRequestId("PKI-Q-001");
        cert.setCertSn("SN-Q-001");
        when(vehicleCertificateRepository.selectById(1L)).thenReturn(cert);
        when(certificateProvisioningAppService.queryCertificateStatus("MES-REQ-Q1"))
                .thenReturn(net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CertificateStatusResult.builder()
                        .requestId("MES-REQ-Q1")
                        .status("ISSUED_NOT_CONFIRMED")
                        .certificateDerBase64("LEAF_DER_B64")
                        .chainDerBase64(new String[]{"CHAIN0_B64"})
                        .build());

        // When
        CertificateCompensateResult result = certificateCompensationAppService.queryCertificate(
                1L, "OP-001", "张三", "10.1.1.1", "UA");

        // Then：只读回填本体，写 QUERY 审计，不改状态
        assertEquals("LEAF_DER_B64", result.getCertificateDerBase64());
        assertArrayEquals(new String[]{"CHAIN0_B64"}, result.getChainDerBase64());
        ArgumentCaptor<VehicleCertificateOperation> opCaptor = ArgumentCaptor.forClass(VehicleCertificateOperation.class);
        verify(vehicleCertificateOperationRepository).insert(opCaptor.capture());
        assertEquals("QUERY", opCaptor.getValue().getAction());
        assertEquals("ISSUED_NOT_CONFIRMED", opCaptor.getValue().getBeforeStatus());
        assertEquals("ISSUED_NOT_CONFIRMED", opCaptor.getValue().getAfterStatus());
    }

    @Test
    @DisplayName("获取证书本体：非签发/激活状态应拒绝且不查询本体")
    void queryCertificate_未签发状态_应拒绝() {
        when(vehicleCertificateRepository.selectById(1L))
                .thenReturn(cert(CertificateStatus.REQUESTED, "MES-REQ-Q2"));
        assertThrows(IllegalStateException.class,
                () -> certificateCompensationAppService.queryCertificate(1L, "OP-001", "张三", "10.1.1.1", "UA"));
        verify(certificateProvisioningAppService, never()).queryCertificateStatus(any());
    }

    // ---------- reissue（重新签发/续期） ----------

    @Test
    @DisplayName("重新签发：作废旧证书后以新requestId重签并写REISSUE审计")
    void reissue_应作废旧证书并重签() {
        // Given：旧记录已签发未确认
        VehicleCertificate old = cert(CertificateStatus.ISSUED_NOT_CONFIRMED, "MES-REQ-OLD");
        when(vehicleCertificateRepository.selectById(1L)).thenReturn(old);
        when(certificateProvisioningAppService.applyDeviceCertificate(any()))
                .thenReturn(CertificateApplyResult.builder()
                        .requestId("MPT-CERT-NEW")
                        .status("ISSUED_NOT_CONFIRMED")
                        .certificateDerBase64("NEW_LEAF_B64")
                        .build());
        VehicleCertificate saved = cert(CertificateStatus.ISSUED_NOT_CONFIRMED, "MPT-CERT-NEW");
        when(vehicleCertificateRepository.selectByRequestId(anyString())).thenReturn(saved);

        // When
        CertificateCompensateResult result = certificateCompensationAppService.reissue(
                1L, csrBase64, "有效期过短续期", "TICKET-RE-001", "OP-001", "张三", "10.1.1.1", "UA");

        // Then：先作废旧证书，再调用签发内核；结果带新证书本体
        verify(certificateProvisioningAppService).supersedeForReissue(eq("MES-REQ-OLD"), eq("OP-001"), any());
        verify(certificateProvisioningAppService).applyDeviceCertificate(any());
        assertEquals("NEW_LEAF_B64", result.getCertificateDerBase64());
        ArgumentCaptor<VehicleCertificateOperation> opCaptor = ArgumentCaptor.forClass(VehicleCertificateOperation.class);
        verify(vehicleCertificateOperationRepository).insert(opCaptor.capture());
        assertEquals("REISSUE", opCaptor.getValue().getAction());
        assertEquals("ISSUED_NOT_CONFIRMED", opCaptor.getValue().getBeforeStatus());
    }

    @Test
    @DisplayName("重新签发：缺reason或ticketNo应拒绝且不作废旧证书")
    void reissue_缺原因或工单_应拒绝() {
        assertThrows(CertificateCompensationReasonRequiredException.class,
                () -> certificateCompensationAppService.reissue(
                        1L, csrBase64, null, "TICKET-RE-001", "OP-001", "张三", "10.1.1.1", "UA"));
        assertThrows(CertificateCompensationReasonRequiredException.class,
                () -> certificateCompensationAppService.reissue(
                        1L, csrBase64, "原因", null, "OP-001", "张三", "10.1.1.1", "UA"));
        verify(certificateProvisioningAppService, never()).supersedeForReissue(any(), any(), any());
        verify(certificateProvisioningAppService, never()).applyDeviceCertificate(any());
    }

    @Test
    @DisplayName("重新签发：缺CSR应拒绝（CSR不落库须重新提供）")
    void reissue_缺CSR_应拒绝() {
        assertThrows(IllegalArgumentException.class,
                () -> certificateCompensationAppService.reissue(
                        1L, null, "续期", "TICKET-RE-001", "OP-001", "张三", "10.1.1.1", "UA"));
        verify(certificateProvisioningAppService, never()).supersedeForReissue(any(), any(), any());
    }

    // ---------- confirmInstalled ----------
    @Test
    @DisplayName("安装补录：缺reason或ticketNo应拒绝（806063）")
    void confirmInstalled_缺原因或工单_应拒绝() {
        assertThrows(CertificateCompensationReasonRequiredException.class,
                () -> certificateCompensationAppService.confirmInstalled(
                        1L, "CERT-001", DEVICE_SN, "SUCCESS", null, null, "TICKET-001",
                        "OP-001", "张三", "10.1.1.1", "UA"));
        assertThrows(CertificateCompensationReasonRequiredException.class,
                () -> certificateCompensationAppService.confirmInstalled(
                        1L, "CERT-001", DEVICE_SN, "SUCCESS", null, "原因", null,
                        "OP-001", "张三", "10.1.1.1", "UA"));
        verify(certificateProvisioningAppService, never()).confirmCertificateInstalled(any());
    }

    @Test
    @DisplayName("安装补录：合法请求应复用共享确认内核并写审计")
    void confirmInstalled_合法请求_应复用共享内核() {
        // Given
        when(vehicleCertificateRepository.selectById(1L))
                .thenReturn(cert(CertificateStatus.ISSUED_NOT_CONFIRMED, "MES-REQ-CF2"));
        VehicleCertificate updated = cert(CertificateStatus.ACTIVE, "MES-REQ-CF2");
        when(vehicleCertificateRepository.selectByRequestId("MES-REQ-CF2")).thenReturn(updated);

        // When
        CertificateCompensateResult result = certificateCompensationAppService.confirmInstalled(
                1L, "CERT-002", DEVICE_SN, "SUCCESS", null, "工位确认", "TICKET-002",
                "OP-001", "张三", "10.1.1.1", "UA");

        // Then
        assertEquals("ACTIVE", result.getStatus());
        verify(certificateProvisioningAppService).confirmCertificateInstalled(any());
        ArgumentCaptor<VehicleCertificateOperation> opCaptor = ArgumentCaptor.forClass(VehicleCertificateOperation.class);
        verify(vehicleCertificateOperationRepository).insert(opCaptor.capture());
        assertEquals("CONFIRM_INSTALLED", opCaptor.getValue().getAction());
        assertEquals("ISSUED_NOT_CONFIRMED", opCaptor.getValue().getBeforeStatus());
        assertEquals("ACTIVE", opCaptor.getValue().getAfterStatus());
        assertEquals("TICKET-002", opCaptor.getValue().getTicketNo());
    }

    // ---------- 查询 ----------

    @Test
    @DisplayName("查询：search 应按条件转发且列表不含CSR全文")
    void search_应按条件查询() {
        when(vehicleCertificateRepository.selectByMap(any())).thenReturn(List.of(cert(CertificateStatus.ACTIVE, "R1")));

        List<net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CertificateListResult> list =
                certificateCompensationAppService.search(VehicleCertificateQuery.builder()
                        .vin(VIN).status("ACTIVE").source("MES").build());
        assertEquals(1, list.size());
        assertEquals(VIN, list.get(0).getVin());
        verify(vehicleCertificateRepository).selectByMap(any());
    }

    @Test
    @DisplayName("查询：getDetail 应返回详情与脱敏操作时间线")
    void getDetail_应返回详情与操作时间线() {
        VehicleCertificate cert = cert(CertificateStatus.ACTIVE, "R-DETAIL");
        when(vehicleCertificateRepository.selectById(9L)).thenReturn(cert);
        when(vehicleCertificateOperationRepository.selectByRequestId("R-DETAIL")).thenReturn(List.of(
                VehicleCertificateOperation.builder()
                        .action("COMPENSATE").operatorName("张三").reason("补申请")
                        .beforeStatus("REQUESTED").afterStatus("ISSUED_NOT_CONFIRMED").result("SUCCESS")
                        .occurredAt(java.time.LocalDateTime.now()).build()));

        CertificateDetailResult detail = certificateCompensationAppService.getDetail(9L);
        assertEquals("R-DETAIL", detail.getRequestId());
        assertEquals(1, detail.getOperations().size());
        assertEquals("COMPENSATE", detail.getOperations().get(0).getAction());
    }

    // ---------- 辅助 ----------

    private CompensateCertificateCmd.CompensateCertificateCmdBuilder baseCmd() {
        return CompensateCertificateCmd.builder()
                .requestId("MES-REQ-053")
                .vin(VIN)
                .deviceCategory("TBOX")
                .deviceSn(DEVICE_SN)
                .certificateProfile(PROFILE)
                .csrDerBase64(csrBase64)
                .reason("人工补偿")
                .operatorId("OP-001")
                .operatorName("张三")
                .sourceIp("10.1.1.1")
                .userAgent("UA");
    }

    private VehicleCertificate cert(CertificateStatus status, String requestId) {
        return VehicleCertificate.builder()
                .id(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE)
                .requestId(requestId)
                .vin(VIN)
                .deviceCategory("TBOX")
                .deviceSn(DEVICE_SN)
                .certificateProfile(PROFILE)
                .certStatus(status)
                .sourceSystem("MES")
                .build();
    }

    private String fingerprintOf(String csr) {
        return net.hwyz.iov.cloud.edd.vmd.service.infrastructure.security.CsrUtils.calculateFingerprint(csr);
    }

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
