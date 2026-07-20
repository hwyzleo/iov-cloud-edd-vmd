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
        when(certEnrollmentTemplateProvider.getIfAvailable()).thenReturn(certificateEnrollmentTemplate);

        applyCmd = CertificateApplyCmd.builder()
                .requestId("REQ-001")
                .vin("HWYZTEST900000001")
                .deviceCategory("TBOX")
                .deviceSn("TBOX-UID-000001")
                .certificateProfile("TBOX_TSP_CLIENT")
                .csrDerBase64(Base64.getEncoder().encodeToString("MOCK_CSR_DATA".getBytes()))
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

        // 使用包含正确设备SN的CSR（以TBOX-开头，以便CsrUtils.parseCommonName能正确解析）
        applyCmd.setCsrDerBase64(Base64.getEncoder().encodeToString("TBOX-UID-000001".getBytes()));

        // Mock PKI调用
        net.hwyz.iov.cloud.framework.security.crypto.model.CertApplyResult frameworkResult = 
                new net.hwyz.iov.cloud.framework.security.crypto.model.CertApplyResult(
                        "PKI-001",
                        EnrollmentState.ISSUED,
                        Instant.now()
                );
        when(certificateEnrollmentTemplate.apply(any())).thenReturn(frameworkResult);

        IssuedCertificate issuedCert = new IssuedCertificate(
                Base64.getEncoder().encode("MOCK_CERT".getBytes()),
                List.of(Base64.getEncoder().encode("MOCK_CHAIN".getBytes())),
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
                .vin("HWYZTEST900000001")
                .deviceSn("TBOX-UID-000001")
                .build();

        // When
        certificateProvisioningAppService.confirmCertificateInstalled(confirmCmd);

        // Then
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

}
