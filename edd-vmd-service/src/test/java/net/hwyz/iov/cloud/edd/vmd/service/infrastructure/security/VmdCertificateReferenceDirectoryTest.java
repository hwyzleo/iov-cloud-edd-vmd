package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.security;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleCertificate;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleCertificateRepository;
import net.hwyz.iov.cloud.framework.security.crypto.exception.CertificateReferenceAmbiguousException;
import net.hwyz.iov.cloud.framework.security.crypto.resolver.CertificateLocator;
import net.hwyz.iov.cloud.framework.security.crypto.resolver.CertificateSubjectRef;
import net.hwyz.iov.cloud.framework.security.crypto.resolver.DeviceCertificateRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * VmdCertificateReferenceDirectory 单元测试（VMD-DSN-CR-060 §4.1）。
 *
 * @author hwyz_leo
 * @since 2026-10-17
 */
@ExtendWith(MockitoExtension.class)
class VmdCertificateReferenceDirectoryTest {

    private static final String DEVICE_SN = "CR060-TBOX-0001";
    private static final String VIN = "HWYZTESTCR06001";

    @Mock
    private VehicleCertificateRepository vehicleCertificateRepository;

    private VmdCertificateReferenceDirectory directory;

    @BeforeEach
    void setUp() {
        directory = new VmdCertificateReferenceDirectory(vehicleCertificateRepository);
    }

    private VehicleCertificate cert(String certSn, String pkiRequestId) {
        return VehicleCertificate.builder()
                .certSn(certSn)
                .pkiRequestId(pkiRequestId)
                .build();
    }

    @Test
    @DisplayName("按 canonical serial 唯一命中 → EnrollmentRequest(requestId)")
    void locate_bySerial_uniqueHit() {
        when(vehicleCertificateRepository.selectByCanonicalCertSn("ab"))
                .thenReturn(List.of(cert("0xAB", "PKI-REQ-0001")));

        Optional<CertificateLocator> locator = directory.locate(DeviceCertificateRef.ofSerial("0xAB"));

        assertTrue(locator.isPresent());
        assertEquals(new CertificateLocator.EnrollmentRequest("PKI-REQ-0001"), locator.get());
    }

    @Test
    @DisplayName("非 canonical serial 变体经 canonical 化后查询仓储")
    void locate_bySerial_canonicalVariantQueriesCanonical() {
        when(vehicleCertificateRepository.selectByCanonicalCertSn("a0b"))
                .thenReturn(List.of(cert("0A:0B", "PKI-REQ-0002")));

        Optional<CertificateLocator> locator = directory.locate(DeviceCertificateRef.ofSerial("0A:0B"));

        assertEquals(Optional.of(new CertificateLocator.EnrollmentRequest("PKI-REQ-0002")), locator);
        verify(vehicleCertificateRepository).selectByCanonicalCertSn("a0b");
    }

    @Test
    @DisplayName("同 canonical serial 命中多条 → CertificateReferenceAmbiguousException")
    void locate_bySerial_ambiguous() {
        when(vehicleCertificateRepository.selectByCanonicalCertSn("ab"))
                .thenReturn(List.of(cert("0xAB", "PKI-REQ-0001"), cert("AB", "PKI-REQ-0002")));

        assertThrows(CertificateReferenceAmbiguousException.class,
                () -> directory.locate(DeviceCertificateRef.ofSerial("0xAB")));
    }

    @Test
    @DisplayName("serial 非空但未命中 → empty，且不回退主体")
    void locate_bySerial_noHit_emptyWithoutSubjectFallback() {
        when(vehicleCertificateRepository.selectByCanonicalCertSn("ab")).thenReturn(List.of());

        Optional<CertificateLocator> locator = directory.locate(
                DeviceCertificateRef.ofSubjectAndSerial(
                        CertificateSubjectRef.ofDevice(DEVICE_SN), "0xAB"));

        assertTrue(locator.isEmpty());
        verify(vehicleCertificateRepository).selectByCanonicalCertSn("ab");
    }

    @Test
    @DisplayName("DEVICE 主体兜底 → 当前 active TBOX_TSP_CLIENT 证书")
    void locate_byDeviceSubject() {
        when(vehicleCertificateRepository.selectActiveByDeviceSnAndProfile(DEVICE_SN, "TBOX_TSP_CLIENT"))
                .thenReturn(cert("0xCD", "PKI-REQ-0003"));

        Optional<CertificateLocator> locator = directory.locate(
                DeviceCertificateRef.ofSubject(CertificateSubjectRef.ofDevice(DEVICE_SN)));

        assertEquals(Optional.of(new CertificateLocator.EnrollmentRequest("PKI-REQ-0003")), locator);
    }

    @Test
    @DisplayName("VEHICLE 主体兜底 → 当前 active TBOX 证书")
    void locate_byVehicleSubject() {
        when(vehicleCertificateRepository.selectActiveByVinAndDeviceCategory(VIN, "TBOX"))
                .thenReturn(cert("0xEF", "PKI-REQ-0004"));

        Optional<CertificateLocator> locator = directory.locate(
                DeviceCertificateRef.ofSubject(CertificateSubjectRef.ofVehicle(VIN)));

        assertEquals(Optional.of(new CertificateLocator.EnrollmentRequest("PKI-REQ-0004")), locator);
    }

    @Test
    @DisplayName("主体无唯一 active 证书 → empty")
    void locate_bySubject_noActive_empty() {
        when(vehicleCertificateRepository.selectActiveByDeviceSnAndProfile(DEVICE_SN, "TBOX_TSP_CLIENT"))
                .thenReturn(null);

        assertTrue(directory.locate(DeviceCertificateRef.ofSubject(CertificateSubjectRef.ofDevice(DEVICE_SN)))
                .isEmpty());
    }

    @Test
    @DisplayName("命中记录缺 pkiRequestId → 视为无 locator（fail-closed）")
    void locate_missingPkiRequestId_empty() {
        when(vehicleCertificateRepository.selectByCanonicalCertSn("ab"))
                .thenReturn(List.of(cert("0xAB", null)));

        assertTrue(directory.locate(DeviceCertificateRef.ofSerial("0xAB")).isEmpty());
    }

    @Test
    @DisplayName("空引用 → empty 且不访问仓储")
    void locate_nullRef_empty() {
        assertTrue(directory.locate(null).isEmpty());
        verifyNoInteractions(vehicleCertificateRepository);
    }

    @Test
    @DisplayName("空 serial + 无主体 → empty")
    void locate_blankSerialNoSubject_empty() {
        assertTrue(directory.locate(DeviceCertificateRef.ofSerial("  ")).isEmpty());
    }
}
