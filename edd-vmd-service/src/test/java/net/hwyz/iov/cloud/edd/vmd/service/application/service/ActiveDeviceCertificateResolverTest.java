package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.common.exception.BusinessKeyDeviceCertNotFoundException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehicleCertificate;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehicleCertificateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * ActiveDeviceCertificateResolver 单元测试（CR-055 §4）
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
@ExtendWith(MockitoExtension.class)
class ActiveDeviceCertificateResolverTest {

    private static final String DEVICE_SN = "00000005AA00000001";
    private static final String CERT_SN = "CR055-CERT-0001";
    private static final String HSM_UID = "00000000000000000000000000000001";

    @Mock
    private VehicleCertificateRepository vehicleCertificateRepository;

    private ActiveDeviceCertificateResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ActiveDeviceCertificateResolver(vehicleCertificateRepository);
    }

    private BoundDeviceIdentity identity() {
        return new BoundDeviceIdentity("HWYZTEST900000001", 1L, 1L, DEVICE_SN, "TBOX", HSM_UID, "BOTH");
    }

    @Test
    void resolveActiveCertSn_ok() {
        VehicleCertificate cert = VehicleCertificate.builder()
                .certSn(CERT_SN)
                .notAfter(LocalDateTime.now().plusDays(365))
                .build();
        when(vehicleCertificateRepository.selectActiveByDeviceSnAndProfile(DEVICE_SN, "TBOX_TSP_CLIENT"))
                .thenReturn(cert);
        assertEquals(CERT_SN, resolver.resolveActiveCertSn(identity()));
    }

    @Test
    void resolveActiveCertSn_missing_rejected() {
        when(vehicleCertificateRepository.selectActiveByDeviceSnAndProfile(anyString(), anyString()))
                .thenReturn(null);
        assertThrows(BusinessKeyDeviceCertNotFoundException.class,
                () -> resolver.resolveActiveCertSn(identity()));
    }

    @Test
    void resolveActiveCertSn_expired_rejected() {
        VehicleCertificate cert = VehicleCertificate.builder()
                .certSn(CERT_SN)
                .notAfter(LocalDateTime.now().minusDays(1))
                .build();
        when(vehicleCertificateRepository.selectActiveByDeviceSnAndProfile(DEVICE_SN, "TBOX_TSP_CLIENT"))
                .thenReturn(cert);
        assertThrows(BusinessKeyDeviceCertNotFoundException.class,
                () -> resolver.resolveActiveCertSn(identity()));
    }
}
