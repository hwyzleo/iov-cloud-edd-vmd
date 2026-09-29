package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.CertificateApplyCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.CertificateConfirmCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CertificateApplyResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.CertificateStatusResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.publish.VehicleDeviceCertificatePublisher;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleCertificate;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehiclePart;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.CertificateStatus;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleCertificateRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehiclePartRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBasicInfoRepository;
import net.hwyz.iov.cloud.framework.security.crypto.CertEnrollmentTemplate;
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
import static org.mockito.Mockito.*;

/**
 * CertificateProvisioningAppService 单元测试
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
class CertificateProvisioningAppServiceTest {

    @Mock
    private VehicleCertificateRepository vehicleCertificateRepository;

    @Mock
    private VehiclePartRepository vehiclePartRepository;

    @Mock
    private PartInfoRepository partInfoRepository;

    @Mock
    private VehBasicInfoRepository vehBasicInfoRepository;

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

    @BeforeEach
    void setUp() {
        lenient().when(certEnrollmentTemplateProvider.getIfAvailable()).thenReturn(certificateEnrollmentTemplate);

        applyCmd = CertificateApplyCmd.builder()
                .requestId("REQ-001")
                .vin("HWYZTEST900000001")
                .deviceCategory("TBOX")
                .deviceSn("TBOX-UID-000001")
                .certificateProfile("TBOX_TSP_CLIENT")
                .csrDerBase64(buildValidCsrBase64("TBOX-UID-000001"))
                .sourceSystem("MES")
                .facilityNo("FA-01")
                .lineCode("LINE-A")
                .build();

        vehiclePart = VehiclePart.builder()
                .id(1L)
                .partId(1L)
                .build();

        vehicleCertificate = VehicleCertificate.builder()
                .id(1L)
                .requestId("REQ-001")
                .vin("HWYZTEST900000001")
                .bindingId(1L)
                .partId(1L)
                .deviceCategory("TBOX")
                .deviceSn("TBOX-UID-000001")
                .certificateProfile("TBOX_TSP_CLIENT")
                .csrFingerprint("CSR_FINGERPRINT")
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
                        .vin("HWYZTEST900000001")
                        .eolTime(Instant.now())
                        .build();
        when(vehBasicInfoRepository.selectByVin("HWYZTEST900000001")).thenReturn(basicInfo);

        // Mock 设备绑定校验
        net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo partInfo = 
                net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo.builder()
                        .id(1L)
                        .sn("TBOX-UID-000001")
                        .build();
        when(partInfoRepository.selectBySn("TBOX-UID-000001")).thenReturn(partInfo);
        when(vehiclePartRepository.selectActiveByVinAndPartId("HWYZTEST900000001", 1L)).thenReturn(vehiclePart);

        // 使用包含正确设备SN的CSR（真实 PKCS#10 自签名，CN=deviceSn）
        // 改用标准 Base64 编码（含 +/ 字符），验证校验/签发两处解码口径一致（不硬编码 URL-safe）
        applyCmd.setCsrDerBase64(toStandardBase64(buildValidCsrBase64("TBOX-UID-000001")));

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
        verify(vehicleCertificateRepository).insert(any(VehicleCertificate.class));
        verify(vehicleCertificateRepository, times(2)).update(any(VehicleCertificate.class));
        verify(certificateEnrollmentTemplate).apply(any());
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
    void applyDeviceCertificate_相同CSR已有有效证书时_应幂等复用不重复签发() {
        // Given：requestId 不存在，但同 (device_sn, profile, csr_fingerprint) 已有 ACTIVE 证书（F15 幂等复用）
        when(vehicleCertificateRepository.selectByRequestId("REQ-001")).thenReturn(null);
        net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo basicInfo =
                net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo.builder()
                        .vin("HWYZTEST900000001")
                        .eolTime(Instant.now())
                        .build();
        when(vehBasicInfoRepository.selectByVin("HWYZTEST900000001")).thenReturn(basicInfo);
        net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo partInfo =
                net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo.builder()
                        .id(1L)
                        .sn("TBOX-UID-000001")
                        .build();
        when(partInfoRepository.selectBySn("TBOX-UID-000001")).thenReturn(partInfo);
        when(vehiclePartRepository.selectActiveByVinAndPartId("HWYZTEST900000001", 1L)).thenReturn(vehiclePart);
        applyCmd.setCsrDerBase64(buildValidCsrBase64("TBOX-UID-000001"));

        VehicleCertificate existingActive = VehicleCertificate.builder()
                .id(9L)
                .requestId("REQ-OLD")
                .vin("HWYZTEST900000001")
                .bindingId(1L)
                .partId(1L)
                .deviceCategory("TBOX")
                .deviceSn("TBOX-UID-000001")
                .certificateProfile("TBOX_TSP_CLIENT")
                .certStatus(CertificateStatus.ACTIVE)
                .build();
        when(vehicleCertificateRepository.selectByDeviceSnAndProfileAndCsrFingerprint(any(), any(), any()))
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
    void applyDeviceCertificate_同requestId并发重复申请_应回读占位行不重复签发() {
        // Given
        net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo basicInfo =
                net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo.builder()
                        .vin("HWYZTEST900000001")
                        .eolTime(Instant.now())
                        .build();
        when(vehBasicInfoRepository.selectByVin("HWYZTEST900000001")).thenReturn(basicInfo);
        net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo partInfo =
                net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo.builder()
                        .id(1L)
                        .sn("TBOX-UID-000001")
                        .build();
        when(partInfoRepository.selectBySn("TBOX-UID-000001")).thenReturn(partInfo);
        when(vehiclePartRepository.selectActiveByVinAndPartId("HWYZTEST900000001", 1L)).thenReturn(vehiclePart);
        applyCmd.setCsrDerBase64(buildValidCsrBase64("TBOX-UID-000001"));
        // 幂等复用查询未命中
        when(vehicleCertificateRepository.selectByDeviceSnAndProfileAndCsrFingerprint(any(), any(), any()))
                .thenReturn(null);
        // 插入命中 uk_request_id 唯一键（并发另一线程已插），回读返回既有占位行
        VehicleCertificate raced = VehicleCertificate.builder()
                .id(9L)
                .requestId("REQ-001")
                .vin("HWYZTEST900000001")
                .bindingId(1L)
                .partId(1L)
                .deviceCategory("TBOX")
                .deviceSn("TBOX-UID-000001")
                .certificateProfile("TBOX_TSP_CLIENT")
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
    void confirmCertificateInstalled_当安装成功时_应更新状态为ACTIVE() {
        // Given
        vehicleCertificate.setCertStatus(CertificateStatus.ISSUED_NOT_CONFIRMED);
        when(vehicleCertificateRepository.selectByRequestId("REQ-001")).thenReturn(vehicleCertificate);

        CertificateConfirmCmd confirmCmd = CertificateConfirmCmd.builder()
                .requestId("REQ-001")
                .result("SUCCESS")
                .vin("HWYZTEST900000001")
                .deviceSn("TBOX-UID-000001")
                .build();

        // When
        certificateProvisioningAppService.confirmCertificateInstalled(confirmCmd);

        // Then
        // 激活新证书前先作废同设备同Profile的旧ACTIVE（落实“最多一条ACTIVE”，§3.1）
        verify(vehicleCertificateRepository).supersedeActiveByDeviceSnAndProfile(
                "TBOX-UID-000001", "TBOX_TSP_CLIENT", "REQ-001");
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
        when(vehicleCertificateRepository.selectActiveByVinAndDeviceCategory("HWYZTEST900000001", "TBOX"))
                .thenReturn(vehicleCertificate);

        // When
        VehicleCertificate result = certificateProvisioningAppService.getActiveCertificateBinding("HWYZTEST900000001", "TBOX");

        // Then
        assertNotNull(result);
        assertEquals(CertificateStatus.ACTIVE, result.getCertStatus());
    }

    @Test
    void getCertificatesByDevice_应返回证书列表() {
        // Given
        when(vehicleCertificateRepository.selectByDeviceSn("TBOX-UID-000001"))
                .thenReturn(List.of(vehicleCertificate));

        // When
        List<VehicleCertificate> result = certificateProvisioningAppService.getCertificatesByDevice("TBOX-UID-000001");

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
    // CSR 强校验（TBOX-SEC-DSN-CR-015 §9.1）：真实解析/验签，非法 CSR 拒签
    // =====================================================================

    @Test
    void applyDeviceCertificate_CSR签名无效时_应拒签() {
        // Given
        when(vehicleCertificateRepository.selectByRequestId("REQ-001")).thenReturn(null);
        net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo basicInfo =
                net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo.builder()
                        .vin("HWYZTEST900000001")
                        .eolTime(Instant.now())
                        .build();
        when(vehBasicInfoRepository.selectByVin("HWYZTEST900000001")).thenReturn(basicInfo);
        net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo partInfo =
                net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo.builder()
                        .id(1L)
                        .sn("TBOX-UID-000001")
                        .build();
        when(partInfoRepository.selectBySn("TBOX-UID-000001")).thenReturn(partInfo);
        when(vehiclePartRepository.selectActiveByVinAndPartId("HWYZTEST900000001", 1L))
                .thenReturn(vehiclePart);

        // CN 正确但签名被篡改：用另一密钥生成 CN 相同但密钥不同的 CSR，再用篡改后的签名验证
        applyCmd.setCsrDerBase64(buildTamperedSignatureCsrBase64("TBOX-UID-000001"));

        // When & Then：签名无效必须拒签（不得继续走 PKI）
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> certificateProvisioningAppService.applyDeviceCertificate(applyCmd));
        assertTrue(ex.getMessage().contains("CSR签名无效"));
        verify(certificateEnrollmentTemplate, never()).apply(any());
    }

    @Test
    void applyDeviceCertificate_CSR畸形时_应拒签() {
        // Given
        when(vehicleCertificateRepository.selectByRequestId("REQ-001")).thenReturn(null);
        net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo basicInfo =
                net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleBasicInfo.builder()
                        .vin("HWYZTEST900000001")
                        .eolTime(Instant.now())
                        .build();
        when(vehBasicInfoRepository.selectByVin("HWYZTEST900000001")).thenReturn(basicInfo);
        net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo partInfo =
                net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartInfo.builder()
                        .id(1L)
                        .sn("TBOX-UID-000001")
                        .build();
        when(partInfoRepository.selectBySn("TBOX-UID-000001")).thenReturn(partInfo);
        when(vehiclePartRepository.selectActiveByVinAndPartId("HWYZTEST900000001", 1L))
                .thenReturn(vehiclePart);

        // 畸形 ASN.1（非 CSR）
        applyCmd.setCsrDerBase64(Base64.getEncoder().encodeToString("NOT_A_REAL_CSR".getBytes()));

        // When & Then：解析失败必须拒签（fail-closed）
        assertThrows(RuntimeException.class,
                () -> certificateProvisioningAppService.applyDeviceCertificate(applyCmd));
        verify(certificateEnrollmentTemplate, never()).apply(any());
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
