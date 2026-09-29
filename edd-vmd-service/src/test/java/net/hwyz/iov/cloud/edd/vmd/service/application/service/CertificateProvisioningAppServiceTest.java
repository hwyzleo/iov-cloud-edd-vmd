package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.CertificateApplyCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.CertificateConfirmCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CertificateApplyResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CertificateStatusResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.publish.VehicleDeviceCertificatePublisher;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateCsrSubjectMismatchException;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateKeyConflictException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleCertificate;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.CertificateStatus;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleCertificateRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.framework.security.crypto.CertEnrollmentTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.exception.PkiOutcomeUnknownException;
import net.hwyz.iov.cloud.framework.security.crypto.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;

/**
 * CertificateProvisioningAppService 单元测试
 * <p>
 * CR-054：身份解析（BoundDeviceIdentityResolver）与 CSR 密码学/CN 校验（CertificateIdentityValidator）
 * 为独立单元，此处 mock；本类聚焦申请编排、幂等复用、换钥冲突与状态推进。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
class CertificateProvisioningAppServiceTest {

    /** TBOX 物理实例序列号（定位绑定，非证书 CN） */
    private static final String DEVICE_SN = "00000005AA00000001";

    /** 权威 HSM UID（证书 CN，32 位十六进制） */
    private static final String HSM_UID = "00000000000000000000000000000001";

    private static final String VIN = "HWYZTEST900000001";
    private static final String PROFILE = "TBOX_TSP_CLIENT";

    @Mock
    private VehicleCertificateRepository vehicleCertificateRepository;

    @Mock
    private VehBasicInfoRepository vehBasicInfoRepository;

    @Mock
    private BoundDeviceIdentityResolver boundDeviceIdentityResolver;

    @Mock
    private CertificateIdentityValidator certificateIdentityValidator;

    @Mock
    private VehicleDeviceCertificatePublisher vehicleDeviceCertificatePublisher;

    @Mock
    private ObjectProvider<CertEnrollmentTemplate> certEnrollmentTemplateProvider;

    @Mock
    private CertEnrollmentTemplate certificateEnrollmentTemplate;

    @InjectMocks
    private CertificateProvisioningAppService certificateProvisioningAppService;

    private CertificateApplyCmd applyCmd;
    private VehicleCertificate vehicleCertificate;
    private VehiclePart vehiclePart;
    private BoundDeviceIdentity identity;

    @BeforeEach
    void setUp() {
        lenient().when(certEnrollmentTemplateProvider.getIfAvailable()).thenReturn(certificateEnrollmentTemplate);

        applyCmd = CertificateApplyCmd.builder()
                .requestId("REQ-001")
                .vin(VIN)
                .deviceCategory("TBOX")
                .deviceSn(DEVICE_SN)
                .certificateProfile(PROFILE)
                .csrDerBase64(buildValidCsrBase64(HSM_UID))
                .sourceSystem("MES")
                .facilityNo("FA-01")
                .lineCode("LINE-A")
                .build();

        vehiclePart = VehiclePart.builder()
                .id(1L)
                .partId(1L)
                .build();

        identity = new BoundDeviceIdentity(VIN, 1L, 1L, DEVICE_SN, "TBOX", HSM_UID, "BOTH");
        lenient().when(boundDeviceIdentityResolver.resolve(VIN, DEVICE_SN, "TBOX")).thenReturn(identity);
        lenient().when(certificateIdentityValidator.validate(any(), any(), any(), any()))
                .thenReturn(new ParsedCsr(HSM_UID, "SPKI_SHA256_MOCK", "CSR_FINGERPRINT_MOCK"));

        vehicleCertificate = VehicleCertificate.builder()
                .id(1L)
                .requestId("REQ-001")
                .vin(VIN)
                .bindingId(1L)
                .partId(1L)
                .deviceCategory("TBOX")
                .deviceSn(DEVICE_SN)
                .hsmUid(HSM_UID)
                .publicKeySha256("SPKI_SHA256_MOCK")
                .certificateProfile(PROFILE)
                .csrFingerprint("CSR_FINGERPRINT_MOCK")
                .certStatus(CertificateStatus.REQUESTED)
                .build();
    }

    @Test
    void applyDeviceCertificate_当申请已存在时_应返回现有状态() {
        // Given
        vehicleCertificate.setCertStatus(CertificateStatus.ISSUING);
        when(vehicleCertificateRepository.selectByRequestId("REQ-001")).thenReturn(vehicleCertificate);

        // When
        CertificateApplyResult result = certificateProvisioningAppService.applyDeviceCertificate(applyCmd);

        // Then
        assertNotNull(result);
        assertEquals("REQ-001", result.getRequestId());
        assertEquals("ISSUING", result.getStatus());
        verify(vehicleCertificateRepository, never()).insert(any());
    }

    @Test
    void applyDeviceCertificate_当新申请时_应创建证书记录并调用PKI() {
        // Given
        when(vehicleCertificateRepository.selectByRequestId("REQ-001")).thenReturn(null);

        // Mock VIN校验
        net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo basicInfo =
                net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo.builder()
                        .vin(VIN)
                        .eolTime(Instant.now())
                        .build();
        when(vehBasicInfoRepository.selectByVin(VIN)).thenReturn(basicInfo);

        // 业务幂等复用未命中、换钥冲突未命中
        when(vehicleCertificateRepository.selectByVinAndUidAndSpkiAndProfile(any(), any(), any(), any()))
                .thenReturn(null);
        when(vehicleCertificateRepository.selectKeyConflictByVinAndUidAndProfile(any(), any(), any(), any()))
                .thenReturn(null);

        // Mock PKI调用
        net.hwyz.iov.cloud.framework.security.crypto.model.CertApplyResult frameworkResult =
                new net.hwyz.iov.cloud.framework.security.crypto.model.CertApplyResult(
                        "PKI-001",
                        EnrollmentState.ISSUED,
                        Instant.now()
                );
        when(certificateEnrollmentTemplate.apply(any())).thenReturn(frameworkResult);

        IssuedCertificate issuedCert = new IssuedCertificate(
                "MOCK_CERT".getBytes(),
                List.of("MOCK_CHAIN".getBytes()),
                "CERT-001",
                Instant.now(),
                Instant.now().plusSeconds(365 * 24 * 60 * 60),
                "SHA256:xxx"
        );
        when(certificateEnrollmentTemplate.getCertificate("PKI-001")).thenReturn(issuedCert);

        // When
        CertificateApplyResult result = certificateProvisioningAppService.applyDeviceCertificate(applyCmd);

        // Then
        assertNotNull(result);
        assertEquals("REQ-001", result.getRequestId());
        assertEquals("ISSUED_NOT_CONFIRMED", result.getStatus());
        // 证书本体必须随响应回传（供产线注入 TBOX，0x31 FF03 写入内容来源）
        assertEquals(Base64.getEncoder().encodeToString("MOCK_CERT".getBytes()), result.getCertificateDerBase64());
        assertNotNull(result.getChainDerBase64());
        assertTrue(result.getChainDerBase64().length >= 1);
        assertEquals(Base64.getEncoder().encodeToString("MOCK_CHAIN".getBytes()), result.getChainDerBase64()[0]);
        assertEquals("CERT-001", result.getCertSn());

        // 落库记录必须携带身份快照（hsm_uid / public_key_sha256，CR-054）
        ArgumentCaptor<VehicleCertificate> certCaptor = ArgumentCaptor.forClass(VehicleCertificate.class);
        verify(vehicleCertificateRepository).insert(certCaptor.capture());
        assertEquals(HSM_UID, certCaptor.getValue().getHsmUid());
        assertEquals("SPKI_SHA256_MOCK", certCaptor.getValue().getPublicKeySha256());

        // framework 申请的 SubjectRef 必须锚定 hsm_uid（CR-054 RD-054-6）
        ArgumentCaptor<CertApplyRequest> reqCaptor = ArgumentCaptor.forClass(CertApplyRequest.class);
        verify(certificateEnrollmentTemplate).apply(reqCaptor.capture());
        assertEquals(SubjectRef.SubjectType.DEVICE_UID, reqCaptor.getValue().subject().type());
        assertEquals(HSM_UID, reqCaptor.getValue().subject().value());

        verify(vehicleCertificateRepository, times(2)).update(any(VehicleCertificate.class));
    }

    @Test
    void applyDeviceCertificate_同身份同公钥已有有效证书时_应幂等复用不重复签发() {
        // Given：requestId 不存在，但同 (vin, hsm_uid, public_key_sha256, profile) 已有 ACTIVE 证书（CR-015 §5）
        when(vehicleCertificateRepository.selectByRequestId("REQ-001")).thenReturn(null);
        net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo basicInfo =
                net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo.builder()
                        .vin(VIN)
                        .eolTime(Instant.now())
                        .build();
        when(vehBasicInfoRepository.selectByVin(VIN)).thenReturn(basicInfo);

        VehicleCertificate existingActive = VehicleCertificate.builder()
                .id(9L)
                .requestId("REQ-OLD")
                .vin(VIN)
                .bindingId(1L)
                .partId(1L)
                .deviceCategory("TBOX")
                .deviceSn(DEVICE_SN)
                .hsmUid(HSM_UID)
                .publicKeySha256("SPKI_SHA256_MOCK")
                .certificateProfile(PROFILE)
                .certStatus(CertificateStatus.ACTIVE)
                .build();
        when(vehicleCertificateRepository.selectByVinAndUidAndSpkiAndProfile(VIN, HSM_UID, "SPKI_SHA256_MOCK", PROFILE))
                .thenReturn(existingActive);

        // When
        CertificateApplyResult result = certificateProvisioningAppService.applyDeviceCertificate(applyCmd);

        // Then：命中有效结果直接复用，不再调 PKI、不再插入新记录
        assertEquals("REQ-OLD", result.getRequestId());
        assertEquals("ACTIVE", result.getStatus());
        verify(certificateEnrollmentTemplate, never()).apply(any());
        verify(vehicleCertificateRepository, never()).insert(any());
    }

    @Test
    void applyDeviceCertificate_同身份不同公钥时_应拒绝签发并提示换钥授权() {
        // Given：同 (vin, hsm_uid, profile) 已存在不同 SPKI 的非终态申请
        when(vehicleCertificateRepository.selectByRequestId("REQ-001")).thenReturn(null);
        net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo basicInfo =
                net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo.builder()
                        .vin(VIN)
                        .eolTime(Instant.now())
                        .build();
        when(vehBasicInfoRepository.selectByVin(VIN)).thenReturn(basicInfo);
        when(vehicleCertificateRepository.selectByVinAndUidAndSpkiAndProfile(any(), any(), any(), any()))
                .thenReturn(null);

        VehicleCertificate otherKey = VehicleCertificate.builder()
                .id(9L)
                .requestId("REQ-OLD")
                .vin(VIN)
                .bindingId(1L)
                .partId(1L)
                .deviceCategory("TBOX")
                .deviceSn(DEVICE_SN)
                .hsmUid(HSM_UID)
                .publicKeySha256("SPKI_OTHER_KEY")
                .certificateProfile(PROFILE)
                .certStatus(CertificateStatus.ACTIVE)
                .build();
        when(vehicleCertificateRepository.selectKeyConflictByVinAndUidAndProfile(VIN, HSM_UID, "SPKI_SHA256_MOCK", PROFILE))
                .thenReturn(otherKey);

        // When & Then：806064，不得返回旧证书、不得调 PKI
        CertificateKeyConflictException ex = assertThrows(CertificateKeyConflictException.class,
                () -> certificateProvisioningAppService.applyDeviceCertificate(applyCmd));
        assertEquals("806064", ex.getErrorCode().getCode());
        verify(certificateEnrollmentTemplate, never()).apply(any());
        verify(vehicleCertificateRepository, never()).insert(any());
    }

    @Test
    void applyDeviceCertificate_身份校验失败时_应传播业务异常且不调PKI() {
        // Given：validator 判定 CN 与绑定 hsm_uid 不一致（负例由 CertificateIdentityValidator 单测覆盖）
        when(vehicleCertificateRepository.selectByRequestId("REQ-001")).thenReturn(null);
        net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo basicInfo =
                net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo.builder()
                        .vin(VIN)
                        .eolTime(Instant.now())
                        .build();
        when(vehBasicInfoRepository.selectByVin(VIN)).thenReturn(basicInfo);
        when(certificateIdentityValidator.validate(any(), any(), any(), any()))
                .thenThrow(new CertificateCsrSubjectMismatchException("CSR Subject CN 与绑定hsm_uid 不一致"));

        // When & Then：806044，PKI 调用次数为 0
        CertificateCsrSubjectMismatchException ex = assertThrows(CertificateCsrSubjectMismatchException.class,
                () -> certificateProvisioningAppService.applyDeviceCertificate(applyCmd));
        assertEquals("806044", ex.getErrorCode().getCode());
        verify(certificateEnrollmentTemplate, never()).apply(any());
        verify(vehicleCertificateRepository, never()).insert(any());
    }

    @Test
    void applyDeviceCertificate_同requestId并发重复申请_应回读占位行不重复签发() {
        // Given
        net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo basicInfo =
                net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo.builder()
                        .vin(VIN)
                        .eolTime(Instant.now())
                        .build();
        when(vehBasicInfoRepository.selectByVin(VIN)).thenReturn(basicInfo);
        // 幂等复用查询未命中、换钥冲突未命中
        when(vehicleCertificateRepository.selectByVinAndUidAndSpkiAndProfile(any(), any(), any(), any()))
                .thenReturn(null);
        when(vehicleCertificateRepository.selectKeyConflictByVinAndUidAndProfile(any(), any(), any(), any()))
                .thenReturn(null);
        // 插入命中 uk_request_id 唯一键（并发另一线程已插），回读返回既有占位行
        VehicleCertificate raced = VehicleCertificate.builder()
                .id(9L)
                .requestId("REQ-001")
                .vin(VIN)
                .bindingId(1L)
                .partId(1L)
                .deviceCategory("TBOX")
                .deviceSn(DEVICE_SN)
                .hsmUid(HSM_UID)
                .publicKeySha256("SPKI_SHA256_MOCK")
                .certificateProfile(PROFILE)
                .certStatus(CertificateStatus.ISSUING)
                .build();
        // 首次 selectByRequestId 返回 null（走到 insert），并发冲突后回读返回占位行
        when(vehicleCertificateRepository.selectByRequestId("REQ-001")).thenReturn(null).thenReturn(raced);
        doThrow(new org.springframework.dao.DuplicateKeyException("uk_request_id"))
                .when(vehicleCertificateRepository).insert(any(VehicleCertificate.class));

        // When
        CertificateApplyResult result = certificateProvisioningAppService.applyDeviceCertificate(applyCmd);

        // Then：回读既有占位行返回，不重复签发
        assertEquals("ISSUING", result.getStatus());
        verify(certificateEnrollmentTemplate, never()).apply(any());
    }

    @Test
    void applyDeviceCertificate_当PKI结果未知异常_应转PENDING_RECONCILE且不抛() {
        // Given：VIN 校验通过、幂等复用/换钥冲突未命中
        net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo basicInfo =
                net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo.builder()
                        .vin(VIN)
                        .eolTime(Instant.now())
                        .build();
        when(vehBasicInfoRepository.selectByVin(VIN)).thenReturn(basicInfo);
        when(vehicleCertificateRepository.selectByVinAndUidAndSpkiAndProfile(any(), any(), any(), any()))
                .thenReturn(null);
        when(vehicleCertificateRepository.selectKeyConflictByVinAndUidAndProfile(any(), any(), any(), any()))
                .thenReturn(null);
        when(vehicleCertificateRepository.selectByRequestId("REQ-001")).thenReturn(null);
        // framework apply 抛结果未知（请求已发送、响应丢失，FW-SEC-DSN-CR-008 §6）
        when(certificateEnrollmentTemplate.apply(any()))
                .thenThrow(new PkiOutcomeUnknownException("PKI sign response lost"));

        // When：结果未知不抛异常（applyWithUnknownHandling 收敛），转待对账
        CertificateApplyResult result = certificateProvisioningAppService.applyDeviceCertificate(applyCmd);

        // Then：PENDING_RECONCILE 引导运维受控恢复，禁止自动重签
        assertEquals("PENDING_RECONCILE", result.getStatus());
        verify(vehicleCertificateRepository, atLeastOnce()).update(argThat(cert ->
                CertificateStatus.PENDING_RECONCILE.equals(cert.getCertStatus())));
    }

    @Test
    void applyDeviceCertificate_当apply返回UNKNOWN状态_应转PENDING_RECONCILE() {
        // Given：幂等命中 UNKNOWN 时 apply 返回 UNKNOWN（不抛异常）
        net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo basicInfo =
                net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo.builder()
                        .vin(VIN)
                        .eolTime(Instant.now())
                        .build();
        when(vehBasicInfoRepository.selectByVin(VIN)).thenReturn(basicInfo);
        when(vehicleCertificateRepository.selectByVinAndUidAndSpkiAndProfile(any(), any(), any(), any()))
                .thenReturn(null);
        when(vehicleCertificateRepository.selectKeyConflictByVinAndUidAndProfile(any(), any(), any(), any()))
                .thenReturn(null);
        when(vehicleCertificateRepository.selectByRequestId("REQ-001")).thenReturn(null);
        when(certificateEnrollmentTemplate.apply(any()))
                .thenReturn(new net.hwyz.iov.cloud.framework.security.crypto.model.CertApplyResult(
                        "PKI-UNKNOWN", EnrollmentState.UNKNOWN, Instant.now()));

        // When
        CertificateApplyResult result = certificateProvisioningAppService.applyDeviceCertificate(applyCmd);

        // Then：UNKNOWN 映射为 PENDING_RECONCILE，pki_request_id 已登记
        assertEquals("PENDING_RECONCILE", result.getStatus());
        assertEquals("PKI-UNKNOWN", result.getPkiRequestId());
        verify(vehicleCertificateRepository, atLeastOnce()).update(argThat(cert ->
                CertificateStatus.PENDING_RECONCILE.equals(cert.getCertStatus())));
    }

    @Test
    void queryCertificateStatus_当申请不存在时_应抛出异常() {
        // Given
        when(vehicleCertificateRepository.selectByRequestId("REQ-001")).thenReturn(null);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            certificateProvisioningAppService.queryCertificateStatus("REQ-001");
        });
    }

    @Test
    void queryCertificateStatus_当申请存在时_应返回状态() {
        // Given
        vehicleCertificate.setCertStatus(CertificateStatus.ACTIVE);
        vehicleCertificate.setCertSn("CERT-001");
        when(vehicleCertificateRepository.selectByRequestId("REQ-001")).thenReturn(vehicleCertificate);

        // When
        CertificateStatusResult result = certificateProvisioningAppService.queryCertificateStatus("REQ-001");

        // Then
        assertNotNull(result);
        assertEquals("REQ-001", result.getRequestId());
        assertEquals("ACTIVE", result.getStatus());
        assertEquals("CERT-001", result.getCertSn());
    }

    @Test
    void queryCertificateStatus_当已签发时_应经PKI重取并回填证书本体() {
        // Given
        vehicleCertificate.setCertStatus(CertificateStatus.ISSUED_NOT_CONFIRMED);
        vehicleCertificate.setCertSn("CERT-001");
        vehicleCertificate.setPkiRequestId("PKI-001");
        vehicleCertificate.setCertificateFingerprint("SHA256:xxx");
        when(vehicleCertificateRepository.selectByRequestId("REQ-001")).thenReturn(vehicleCertificate);

        IssuedCertificate issuedCert = new IssuedCertificate(
                "MOCK_CERT".getBytes(),
                List.of("MOCK_CHAIN".getBytes()),
                "CERT-001",
                Instant.now(),
                Instant.now().plusSeconds(365 * 24 * 60 * 60),
                "SHA256:xxx"
        );
        when(certificateEnrollmentTemplate.getCertificate("PKI-001")).thenReturn(issuedCert);

        // When
        CertificateStatusResult result = certificateProvisioningAppService.queryCertificateStatus("REQ-001");

        // Then
        assertNotNull(result);
        assertEquals("ISSUED_NOT_CONFIRMED", result.getStatus());
        assertEquals("CERT-001", result.getCertSn());
        assertEquals(Base64.getEncoder().encodeToString("MOCK_CERT".getBytes()), result.getCertificateDerBase64());
        assertNotNull(result.getChainDerBase64());
        assertEquals(Base64.getEncoder().encodeToString("MOCK_CHAIN".getBytes()), result.getChainDerBase64()[0]);
        verify(certificateEnrollmentTemplate).getCertificate("PKI-001");
    }

    @Test
    void queryCertificateStatus_当已签发但PKI重取失败时_应仅返回元数据() {
        // Given
        vehicleCertificate.setCertStatus(CertificateStatus.ACTIVE);
        vehicleCertificate.setCertSn("CERT-001");
        vehicleCertificate.setPkiRequestId("PKI-001");
        when(vehicleCertificateRepository.selectByRequestId("REQ-001")).thenReturn(vehicleCertificate);
        when(certificateEnrollmentTemplate.getCertificate("PKI-001"))
                .thenThrow(new IllegalStateException("PKI不可达"));

        // When
        CertificateStatusResult result = certificateProvisioningAppService.queryCertificateStatus("REQ-001");

        // Then
        assertNotNull(result);
        assertEquals("ACTIVE", result.getStatus());
        assertEquals("CERT-001", result.getCertSn());
        assertNull(result.getCertificateDerBase64());
        assertNull(result.getChainDerBase64());
    }


    @Test
    void confirmCertificateInstalled_当状态不是ISSUED_NOT_CONFIRMED时_应抛出异常() {
        // Given
        vehicleCertificate.setCertStatus(CertificateStatus.ISSUING);
        when(vehicleCertificateRepository.selectByRequestId("REQ-001")).thenReturn(vehicleCertificate);

        CertificateConfirmCmd confirmCmd = CertificateConfirmCmd.builder()
                .requestId("REQ-001")
                .result("SUCCESS")
                .build();

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            certificateProvisioningAppService.confirmCertificateInstalled(confirmCmd);
        });
    }

    @Test
    void confirmCertificateInstalled_当安装成功时_应更新状态为ACTIVE() {
        // Given
        vehicleCertificate.setCertStatus(CertificateStatus.ISSUED_NOT_CONFIRMED);
        when(vehicleCertificateRepository.selectByRequestId("REQ-001")).thenReturn(vehicleCertificate);

        CertificateConfirmCmd confirmCmd = CertificateConfirmCmd.builder()
                .requestId("REQ-001")
                .result("SUCCESS")
                .vin(VIN)
                .deviceSn(DEVICE_SN)
                .build();

        // When
        certificateProvisioningAppService.confirmCertificateInstalled(confirmCmd);

        // Then
        // 激活新证书前先作废同设备同Profile的旧ACTIVE（落实“最多一条ACTIVE”，§3.1）
        verify(vehicleCertificateRepository).supersedeActiveByDeviceSnAndProfile(
                DEVICE_SN, PROFILE, "REQ-001");
        verify(vehicleCertificateRepository).update(argThat(cert ->
                CertificateStatus.ACTIVE.equals(cert.getCertStatus()) && cert.getConfirmedAt() != null));
        verify(vehicleDeviceCertificatePublisher).publishCertificateChanged(any());
    }

    @Test
    void confirmCertificateInstalled_当安装失败时_应更新状态为INSTALL_FAILED() {
        // Given
        vehicleCertificate.setCertStatus(CertificateStatus.ISSUED_NOT_CONFIRMED);
        when(vehicleCertificateRepository.selectByRequestId("REQ-001")).thenReturn(vehicleCertificate);

        CertificateConfirmCmd confirmCmd = CertificateConfirmCmd.builder()
                .requestId("REQ-001")
                .result("FAILED")
                .failReason("安装超时")
                .build();

        // When
        certificateProvisioningAppService.confirmCertificateInstalled(confirmCmd);

        // Then
        verify(vehicleCertificateRepository).update(argThat(cert ->
                CertificateStatus.INSTALL_FAILED.equals(cert.getCertStatus()) && "安装超时".equals(cert.getFailReason())));
        verify(vehicleDeviceCertificatePublisher).publishCertificateChanged(any());
    }

    @Test
    void getActiveCertificateBinding_应返回活跃证书() {
        // Given
        vehicleCertificate.setCertStatus(CertificateStatus.ACTIVE);
        when(vehicleCertificateRepository.selectActiveByVinAndDeviceCategory(VIN, "TBOX"))
                .thenReturn(vehicleCertificate);

        // When
        VehicleCertificate result = certificateProvisioningAppService.getActiveCertificateBinding(VIN, "TBOX");

        // Then
        assertNotNull(result);
        assertEquals(CertificateStatus.ACTIVE, result.getCertStatus());
    }

    @Test
    void getCertificatesByDevice_应返回证书列表() {
        // Given
        when(vehicleCertificateRepository.selectByDeviceSn(DEVICE_SN))
                .thenReturn(List.of(vehicleCertificate));

        // When
        List<VehicleCertificate> result = certificateProvisioningAppService.getCertificatesByDevice(DEVICE_SN);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getCertificateBySerial_应返回证书() {
        // Given
        vehicleCertificate.setCertSn("CERT-001");
        when(vehicleCertificateRepository.selectByCertSn("CERT-001"))
                .thenReturn(vehicleCertificate);

        // When
        VehicleCertificate result = certificateProvisioningAppService.getCertificateBySerial("CERT-001");

        // Then
        assertNotNull(result);
        assertEquals("CERT-001", result.getCertSn());
    }

    // =====================================================================
    // CR-053：reconcile 对账 + confirm 共享内核扩展（US-060）
    // =====================================================================

    @Test
    void reconcile_有pkiRequestId且PKI已签发_应推进至ISSUED_NOT_CONFIRMED() {
        // Given：ISSUING + 有 pki_request_id，PKI 状态已 ISSUED
        vehicleCertificate.setCertStatus(CertificateStatus.ISSUING);
        vehicleCertificate.setPkiRequestId("PKI-001");
        when(vehicleCertificateRepository.selectByRequestId("REQ-001")).thenReturn(vehicleCertificate);
        when(vehicleCertificateRepository.selectByIdForUpdate(1L)).thenReturn(vehicleCertificate);
        when(certificateEnrollmentTemplate.getStatus("PKI-001"))
                .thenReturn(new net.hwyz.iov.cloud.framework.security.crypto.model.CertApplyResult(
                        "PKI-001", EnrollmentState.ISSUED, Instant.now()));
        IssuedCertificate issuedCert = new IssuedCertificate(
                "MOCK_CERT".getBytes(), List.of("MOCK_CHAIN".getBytes()), "CERT-001",
                Instant.now(), Instant.now().plusSeconds(365 * 24 * 60 * 60), "SHA256:xxx");
        when(certificateEnrollmentTemplate.getCertificate("PKI-001")).thenReturn(issuedCert);

        // When
        CertificateApplyResult result = certificateProvisioningAppService.reconcile("REQ-001", null, "OP-001", "张三");

        // Then：对账推进至 ISSUED_NOT_CONFIRMED，不生成新键、不重新 apply
        assertEquals("ISSUED_NOT_CONFIRMED", result.getStatus());
        assertEquals("CERT-001", result.getCertSn());
        verify(certificateEnrollmentTemplate, never()).apply(any());
        verify(vehicleCertificateRepository).selectByIdForUpdate(1L);
    }

    @Test
    void reconcile_无pkiRequestId且无CSR_应转PENDING_RECONCILE() {
        // Given：REQUESTED + 无 pki_request_id（崩溃残留），MPT 仅传 id（无 CSR）
        vehicleCertificate.setCertStatus(CertificateStatus.REQUESTED);
        vehicleCertificate.setPkiRequestId(null);
        when(vehicleCertificateRepository.selectByRequestId("REQ-001")).thenReturn(vehicleCertificate);
        when(vehicleCertificateRepository.selectByIdForUpdate(1L)).thenReturn(vehicleCertificate);

        // When
        CertificateApplyResult result = certificateProvisioningAppService.reconcile("REQ-001", null, "OP-001", "张三");

        // Then：CSR 全文不落库，无 CSR 无法按原键重发，转待对账引导人工补申请
        assertEquals("PENDING_RECONCILE", result.getStatus());
        verify(certificateEnrollmentTemplate, never()).apply(any());
        verify(vehicleCertificateRepository, times(2)).update(argThat(cert ->
                CertificateStatus.PENDING_RECONCILE.equals(cert.getCertStatus())));
    }

    @Test
    void reconcile_无pkiRequestId但有CSR_应复用原键重新apply() {
        // Given：REQUESTED + 无 pki_request_id，操作员携带受信工位回读 CSR（人工补申请继续签发）
        vehicleCertificate.setCertStatus(CertificateStatus.REQUESTED);
        vehicleCertificate.setPkiRequestId(null);
        vehicleCertificate.setDeviceSn(DEVICE_SN);
        vehicleCertificate.setCertificateProfile(PROFILE);
        when(vehicleCertificateRepository.selectByRequestId("REQ-001")).thenReturn(vehicleCertificate);
        when(vehicleCertificateRepository.selectByIdForUpdate(1L)).thenReturn(vehicleCertificate);
        String csr = buildValidCsrBase64(HSM_UID);
        when(certificateEnrollmentTemplate.apply(any()))
                .thenReturn(new net.hwyz.iov.cloud.framework.security.crypto.model.CertApplyResult(
                        "PKI-053", EnrollmentState.ISSUED, Instant.now()));
        IssuedCertificate issuedCert = new IssuedCertificate(
                "MOCK_CERT".getBytes(), List.of("MOCK_CHAIN".getBytes()), "CERT-053",
                Instant.now(), Instant.now().plusSeconds(365 * 24 * 60 * 60), "SHA256:xxx");
        when(certificateEnrollmentTemplate.getCertificate("PKI-053")).thenReturn(issuedCert);

        // When
        CertificateApplyResult result = certificateProvisioningAppService.reconcile("REQ-001", csr, "OP-001", "张三");

        // Then：以原 requestId 作 idempotencyKey 重新 apply，未生成新键；SubjectRef 锚 hsm_uid
        assertEquals("ISSUED_NOT_CONFIRMED", result.getStatus());
        ArgumentCaptor<CertApplyRequest> reqCaptor = ArgumentCaptor.forClass(CertApplyRequest.class);
        verify(certificateEnrollmentTemplate).apply(reqCaptor.capture());
        assertEquals("REQ-001", reqCaptor.getValue().idempotencyKey());
        assertEquals(SubjectRef.SubjectType.DEVICE_UID, reqCaptor.getValue().subject().type());
        assertEquals(HSM_UID, reqCaptor.getValue().subject().value());
        // 存量行缺身份快照时回填（reconcile/doFrameworkApply/updateCertificateFromIssued 均携带同一对象，至少一次）
        verify(vehicleCertificateRepository, atLeastOnce()).update(argThat(cert ->
                HSM_UID.equals(cert.getHsmUid()) && "SPKI_SHA256_MOCK".equals(cert.getPublicKeySha256())));
    }

    @Test
    void reconcile_FAILED无pki但有CSR_应按原键重新apply重试() {
        // Given：FAILED 终态失败 + 无 pki_request_id，操作员携带 CSR 失败重试
        vehicleCertificate.setCertStatus(CertificateStatus.FAILED);
        vehicleCertificate.setPkiRequestId(null);
        vehicleCertificate.setDeviceSn(DEVICE_SN);
        vehicleCertificate.setCertificateProfile(PROFILE);
        when(vehicleCertificateRepository.selectByRequestId("REQ-001")).thenReturn(vehicleCertificate);
        when(vehicleCertificateRepository.selectByIdForUpdate(1L)).thenReturn(vehicleCertificate);
        String csr = buildValidCsrBase64(HSM_UID);
        when(certificateEnrollmentTemplate.apply(any()))
                .thenReturn(new net.hwyz.iov.cloud.framework.security.crypto.model.CertApplyResult(
                        "PKI-053-RETRY", EnrollmentState.ISSUED, Instant.now()));
        IssuedCertificate issuedCert = new IssuedCertificate(
                "MOCK_CERT".getBytes(), List.of("MOCK_CHAIN".getBytes()), "CERT-053-RETRY",
                Instant.now(), Instant.now().plusSeconds(365 * 24 * 60 * 60), "SHA256:xxx");
        when(certificateEnrollmentTemplate.getCertificate("PKI-053-RETRY")).thenReturn(issuedCert);

        // When
        CertificateApplyResult result = certificateProvisioningAppService.reconcile("REQ-001", csr, "OP-001", "张三");

        // Then：FAILED 按原 requestId 重新 apply，重试成功推进至 ISSUED_NOT_CONFIRMED
        assertEquals("ISSUED_NOT_CONFIRMED", result.getStatus());
        ArgumentCaptor<CertApplyRequest> reqCaptor = ArgumentCaptor.forClass(CertApplyRequest.class);
        verify(certificateEnrollmentTemplate).apply(reqCaptor.capture());
        assertEquals("REQ-001", reqCaptor.getValue().idempotencyKey());
    }

    @Test
    void reconcile_FAILED有pki但有CSR_应优先重新apply而非getStatus() {
        // Given：FAILED + 已有 pki_request_id（PKI 曾明确失败/拒绝），操作员携带 CSR 失败重试
        vehicleCertificate.setCertStatus(CertificateStatus.FAILED);
        vehicleCertificate.setPkiRequestId("PKI-OLD-FAILED");
        vehicleCertificate.setDeviceSn(DEVICE_SN);
        vehicleCertificate.setCertificateProfile(PROFILE);
        when(vehicleCertificateRepository.selectByRequestId("REQ-001")).thenReturn(vehicleCertificate);
        when(vehicleCertificateRepository.selectByIdForUpdate(1L)).thenReturn(vehicleCertificate);
        String csr = buildValidCsrBase64(HSM_UID);
        when(certificateEnrollmentTemplate.apply(any()))
                .thenReturn(new net.hwyz.iov.cloud.framework.security.crypto.model.CertApplyResult(
                        "PKI-053-RETRY2", EnrollmentState.ISSUED, Instant.now()));
        IssuedCertificate issuedCert = new IssuedCertificate(
                "MOCK_CERT".getBytes(), List.of("MOCK_CHAIN".getBytes()), "CERT-053-RETRY2",
                Instant.now(), Instant.now().plusSeconds(365 * 24 * 60 * 60), "SHA256:xxx");
        when(certificateEnrollmentTemplate.getCertificate("PKI-053-RETRY2")).thenReturn(issuedCert);

        // When
        CertificateApplyResult result = certificateProvisioningAppService.reconcile("REQ-001", csr, "OP-001", "张三");

        // Then：FAILED 优先按原键重新 apply，不走向旧 pki 的 getStatus 对账
        assertEquals("ISSUED_NOT_CONFIRMED", result.getStatus());
        verify(certificateEnrollmentTemplate).apply(any());
        verify(certificateEnrollmentTemplate, never()).getStatus(any());
    }

    @Test
    void reconcile_当getStatus返回UNKNOWN_应转PENDING_RECONCILE() {
        // Given：ISSUING + pki_request_id，PKI 状态查询返回结果未知
        vehicleCertificate.setCertStatus(CertificateStatus.ISSUING);
        vehicleCertificate.setPkiRequestId("PKI-053-UNK");
        when(vehicleCertificateRepository.selectByRequestId("REQ-001")).thenReturn(vehicleCertificate);
        when(vehicleCertificateRepository.selectByIdForUpdate(1L)).thenReturn(vehicleCertificate);
        when(certificateEnrollmentTemplate.getStatus("PKI-053-UNK"))
                .thenReturn(new net.hwyz.iov.cloud.framework.security.crypto.model.CertApplyResult(
                        "PKI-053-UNK", EnrollmentState.UNKNOWN, Instant.now()));

        // When
        CertificateApplyResult result = certificateProvisioningAppService.reconcile("REQ-001", null, "OP-001", "张三");

        // Then：结果未知禁止自动重签，转待对账
        assertEquals("PENDING_RECONCILE", result.getStatus());
        verify(certificateEnrollmentTemplate, never()).apply(any());
        verify(vehicleCertificateRepository, atLeastOnce()).update(argThat(cert ->
                CertificateStatus.PENDING_RECONCILE.equals(cert.getCertStatus())));
    }

    @Test
    void reconcile_FAILED重试PKI结果未知_应转PENDING_RECONCILE且不抛() {
        // Given：FAILED + 无 pki，操作员携带 CSR 失败重试，PKI 结果未知
        vehicleCertificate.setCertStatus(CertificateStatus.FAILED);
        vehicleCertificate.setPkiRequestId(null);
        vehicleCertificate.setDeviceSn(DEVICE_SN);
        vehicleCertificate.setCertificateProfile(PROFILE);
        when(vehicleCertificateRepository.selectByRequestId("REQ-001")).thenReturn(vehicleCertificate);
        when(vehicleCertificateRepository.selectByIdForUpdate(1L)).thenReturn(vehicleCertificate);
        String csr = buildValidCsrBase64(HSM_UID);
        when(certificateEnrollmentTemplate.apply(any()))
                .thenThrow(new PkiOutcomeUnknownException("PKI sign response lost on retry"));

        // When：结果未知不抛异常（applyWithUnknownHandling 收敛），转待对账
        CertificateApplyResult result = certificateProvisioningAppService.reconcile("REQ-001", csr, "OP-001", "张三");

        // Then
        assertEquals("PENDING_RECONCILE", result.getStatus());
        verify(certificateEnrollmentTemplate).apply(any());
        verify(vehicleCertificateRepository, atLeastOnce()).update(argThat(cert ->
                CertificateStatus.PENDING_RECONCILE.equals(cert.getCertStatus())));
    }

    @Test
    void reconcile_状态为终态_应抛补偿不允许() {
        // Given：ACTIVE 终态禁止对账回退
        vehicleCertificate.setCertStatus(CertificateStatus.ACTIVE);
        vehicleCertificate.setPkiRequestId("PKI-001");
        when(vehicleCertificateRepository.selectByRequestId("REQ-001")).thenReturn(vehicleCertificate);
        when(vehicleCertificateRepository.selectByIdForUpdate(1L)).thenReturn(vehicleCertificate);

        // When & Then：806060
        net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateCompensationNotAllowedException ex =
                assertThrows(net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateCompensationNotAllowedException.class,
                        () -> certificateProvisioningAppService.reconcile("REQ-001", null, "OP-001", "张三"));
        assertEquals("806060", ex.getErrorCode().getCode());
        verify(certificateEnrollmentTemplate, never()).apply(any());
        verify(certificateEnrollmentTemplate, never()).getStatus(any());
    }

    @Test
    void confirmCertificateInstalled_当状态为INSTALL_FAILED时_允许重试成功() {
        // Given：CR-053 共享内核允许 INSTALL_FAILED 在重新注入/校验后重试确认
        vehicleCertificate.setCertStatus(CertificateStatus.INSTALL_FAILED);
        vehicleCertificate.setCertSn("CERT-001");
        when(vehicleCertificateRepository.selectByRequestId("REQ-001")).thenReturn(vehicleCertificate);

        CertificateConfirmCmd confirmCmd = CertificateConfirmCmd.builder()
                .requestId("REQ-001")
                .result("SUCCESS")
                .certSn("CERT-001")
                .deviceSn(DEVICE_SN)
                .reason("重新注入后重试")
                .ticketNo("TICKET-053")
                .operatorId("OP-001")
                .build();

        // When
        certificateProvisioningAppService.confirmCertificateInstalled(confirmCmd);

        // Then：INSTALL_FAILED → ACTIVE，审计上下文落库
        verify(vehicleCertificateRepository).update(argThat(cert ->
                CertificateStatus.ACTIVE.equals(cert.getCertStatus())
                        && "OP-001".equals(cert.getLastOperator())
                        && "TICKET-053".equals(cert.getTicketNo())
                        && "重新注入后重试".equals(cert.getCompensationReason())
                        && cert.getLastOperationAt() != null));
        verify(vehicleDeviceCertificatePublisher).publishCertificateChanged(any());
    }

    @Test
    void confirmCertificateInstalled_当certSn不匹配时_应拒绝() {
        // Given：CR-053 共享内核校验 requestId + certSn + deviceSn
        vehicleCertificate.setCertStatus(CertificateStatus.ISSUED_NOT_CONFIRMED);
        vehicleCertificate.setCertSn("CERT-001");
        when(vehicleCertificateRepository.selectByRequestId("REQ-001")).thenReturn(vehicleCertificate);

        CertificateConfirmCmd confirmCmd = CertificateConfirmCmd.builder()
                .requestId("REQ-001")
                .result("SUCCESS")
                .certSn("CERT-WRONG")
                .deviceSn(DEVICE_SN)
                .build();

        // When & Then
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> certificateProvisioningAppService.confirmCertificateInstalled(confirmCmd));
        assertTrue(ex.getMessage().contains("证书序列号不匹配"));
        verify(vehicleCertificateRepository, never()).update(any());
        verify(vehicleDeviceCertificatePublisher, never()).publishCertificateChanged(any());
    }

    @Test
    void confirmCertificateInstalled_终态ACTIVE禁止回退() {
        // Given：终态不得回退
        vehicleCertificate.setCertStatus(CertificateStatus.ACTIVE);
        when(vehicleCertificateRepository.selectByRequestId("REQ-001")).thenReturn(vehicleCertificate);

        CertificateConfirmCmd confirmCmd = CertificateConfirmCmd.builder()
                .requestId("REQ-001")
                .result("SUCCESS")
                .build();

        // When & Then
        assertThrows(IllegalStateException.class,
                () -> certificateProvisioningAppService.confirmCertificateInstalled(confirmCmd));
        verify(vehicleCertificateRepository, never()).update(any());
    }

    /**
     * 将 URL-safe（无 padding）Base64 转换为标准 Base64（含 +/ 与 padding），
     * 用于验证校验/签发两处解码口径对标准 Base64 一致（不硬编码 URL-safe）。
     */
    private static String toStandardBase64(String urlSafe) {
        String standard = urlSafe.replace('-', '+').replace('_', '/');
        int pad = (4 - standard.length() % 4) % 4;
        return pad == 0 ? standard : standard + "=".repeat(pad);
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
            extGen.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
            extGen.addExtension(Extension.extendedKeyUsage, false,
                    new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth));
            builder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extGen.generate());
            ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA").build(kp.getPrivate());
            PKCS10CertificationRequest req = builder.build(signer);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(req.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("生成测试CSR失败", e);
        }
    }

    /**
     * 生成签名无效的 CSR：用另一把私钥重新签名（同一公钥、不同私钥不可能产生，
     * 故这里构造“公钥与签名不匹配”的 CSR：用 keyA 的公钥、keyB 的私钥签名）
     */
    private static String buildTamperedSignatureCsrBase64(String cn) {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(new ECGenParameterSpec("secp256r1"));
            KeyPair pubKeyPair = kpg.generateKeyPair();   // 公钥来源 A
            KeyPair signKeyPair = kpg.generateKeyPair();  // 签名来源 B（不同私钥）
            X500Name subject = new X500Name("CN=" + cn + ",OU=TBOX-TSP,O=OpenIOV,C=CN");
            PKCS10CertificationRequestBuilder builder = new PKCS10CertificationRequestBuilder(
                    subject, SubjectPublicKeyInfo.getInstance(pubKeyPair.getPublic().getEncoded()));
            ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA").build(signKeyPair.getPrivate());
            PKCS10CertificationRequest req = builder.build(signer);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(req.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("生成篡改CSR失败", e);
        }
    }

}
