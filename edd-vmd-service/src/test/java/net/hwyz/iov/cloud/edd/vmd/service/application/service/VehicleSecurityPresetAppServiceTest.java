package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehSecurityConstant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SecurityConstantState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehImportDataRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehSecurityConstantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * VehicleSecurityPresetAppService 单元测试
 * <p>
 * VMD-DSN-CR-028: 车辆安全常量预置应用服务测试
 *
 * @author hwyz_leo
 * @since 2026-06-17
 */
@ExtendWith(MockitoExtension.class)
class VehicleSecurityPresetAppServiceTest {

    @Mock
    private VehSecurityConstantRepository vehSecurityConstantRepository;

    @Mock
    private VehImportDataRepository vehImportDataRepository;

    private VehicleSecurityPresetAppService vehicleSecurityPresetAppService;

    @BeforeEach
    void setUp() {
        vehicleSecurityPresetAppService = new VehicleSecurityPresetAppService(
                vehSecurityConstantRepository, vehImportDataRepository);
    }

    @Test
    @DisplayName("应成功预置新车辆安全常量")
    void testPresetSuccessForNewVehicle() {
        // Given
        String vin = "TEST_VIN_001";
        String batchNum = "BATCH_001";

        when(vehSecurityConstantRepository.selectByVin(vin)).thenReturn(null);
        when(vehSecurityConstantRepository.insert(any(VehSecurityConstant.class))).thenReturn(1);
        when(vehSecurityConstantRepository.update(any(VehSecurityConstant.class))).thenReturn(1);

        // When
        vehicleSecurityPresetAppService.preset(vin, batchNum);

        // Then
        verify(vehSecurityConstantRepository).selectByVin(vin);
        verify(vehSecurityConstantRepository).insert(any(VehSecurityConstant.class));
        verify(vehSecurityConstantRepository).update(argThat(entity -> {
            assertEquals(SecurityConstantState.PRESET, entity.getPresetState());
            assertNotNull(entity.getKmsKeyRef());
            assertNotNull(entity.getGenTime());
            assertNotNull(entity.getLastAttemptTime());
            return true;
        }));
    }

    @Test
    @DisplayName("应跳过已预置的车辆安全常量（幂等检查）")
    void testPresetSkipAlreadyPresetVehicle() {
        // Given
        String vin = "TEST_VIN_002";
        String batchNum = "BATCH_002";

        VehSecurityConstant existing = VehSecurityConstant.builder()
                .id(1L)
                .vin(vin)
                .batchNum(batchNum)
                .presetState(SecurityConstantState.PRESET)
                .constantType("SECURITY_KEY")
                .createTime(LocalDateTime.now())
                .build();

        when(vehSecurityConstantRepository.selectByVin(vin)).thenReturn(existing);

        // When
        vehicleSecurityPresetAppService.preset(vin, batchNum);

        // Then
        verify(vehSecurityConstantRepository).selectByVin(vin);
        verify(vehSecurityConstantRepository, never()).insert(any());
        verify(vehSecurityConstantRepository, never()).update(any());
    }

    @Test
    @DisplayName("应更新已存在但未预置的车辆安全常量")
    void testPresetUpdateExistingNonPresetVehicle() {
        // Given
        String vin = "TEST_VIN_003";
        String batchNum = "BATCH_003";

        VehSecurityConstant existing = VehSecurityConstant.builder()
                .id(1L)
                .vin(vin)
                .batchNum("OLD_BATCH")
                .presetState(SecurityConstantState.FAILED)
                .constantType("SECURITY_KEY")
                .createTime(LocalDateTime.now())
                .build();

        when(vehSecurityConstantRepository.selectByVin(vin)).thenReturn(existing);
        when(vehSecurityConstantRepository.update(any(VehSecurityConstant.class))).thenReturn(1);

        // When
        vehicleSecurityPresetAppService.preset(vin, batchNum);

        // Then
        verify(vehSecurityConstantRepository).selectByVin(vin);
        verify(vehSecurityConstantRepository, never()).insert(any());
        verify(vehSecurityConstantRepository).update(argThat(entity -> {
            assertEquals(SecurityConstantState.PRESET, entity.getPresetState());
            assertEquals(batchNum, entity.getBatchNum());
            return true;
        }));
    }

    @Test
    @DisplayName("应正确设置createTime和constantType")
    void testPresetSetsCreateTimeAndConstantType() {
        // Given
        String vin = "TEST_VIN_004";
        String batchNum = "BATCH_004";

        when(vehSecurityConstantRepository.selectByVin(vin)).thenReturn(null);
        when(vehSecurityConstantRepository.insert(any(VehSecurityConstant.class))).thenReturn(1);
        when(vehSecurityConstantRepository.update(any(VehSecurityConstant.class))).thenReturn(1);

        // When
        vehicleSecurityPresetAppService.preset(vin, batchNum);

        // Then - verify insert was called with correct initial values
        verify(vehSecurityConstantRepository).insert(argThat(entity -> {
            assertEquals(vin, entity.getVin());
            assertEquals(batchNum, entity.getBatchNum());
            assertEquals("SECURITY_KEY", entity.getConstantType());
            assertNotNull(entity.getCreateTime());
            return true;
        }));
    }

    @Test
    @DisplayName("应正确截断超长failReason")
    void testTruncateLongFailReason() {
        // Given
        String vin = "TEST_VIN_005";
        String batchNum = "BATCH_005";

        StringBuilder longError = new StringBuilder();
        for (int i = 0; i < 600; i++) {
            longError.append("A");
        }

        when(vehSecurityConstantRepository.selectByVin(vin)).thenReturn(null);
        when(vehSecurityConstantRepository.insert(any(VehSecurityConstant.class))).thenReturn(1);
        // Make first update throw to simulate failure in KMS/HSM call
        // Second update (in handlePresetFailure) will succeed
        when(vehSecurityConstantRepository.update(any(VehSecurityConstant.class)))
                .thenThrow(new RuntimeException(longError.toString()))
                .thenReturn(1);
        when(vehImportDataRepository.selectByBatchNum(batchNum)).thenReturn(null);

        // When & Then - should not throw
        assertDoesNotThrow(() -> vehicleSecurityPresetAppService.preset(vin, batchNum));

        // Capture all update calls
        ArgumentCaptor<VehSecurityConstant> updateCaptor = ArgumentCaptor.forClass(VehSecurityConstant.class);
        verify(vehSecurityConstantRepository, times(2)).update(updateCaptor.capture());

        // Verify the second call (in handlePresetFailure) has truncated failReason
        List<VehSecurityConstant> allUpdates = updateCaptor.getAllValues();
        VehSecurityConstant handlePresetFailureUpdate = allUpdates.get(1);
        assertEquals(SecurityConstantState.FAILED, handlePresetFailureUpdate.getPresetState());
        assertNotNull(handlePresetFailureUpdate.getFailReason());
        assertTrue(handlePresetFailureUpdate.getFailReason().length() <= 500);
        assertTrue(handlePresetFailureUpdate.getFailReason().endsWith("..."));
    }

    @Test
    @DisplayName("应正确截断超长description")
    void testTruncateLongDescription() {
        // Given
        String vin = "TEST_VIN_006";
        String batchNum = "BATCH_006";

        StringBuilder longDescription = new StringBuilder();
        for (int i = 0; i < 400; i++) {
            longDescription.append("B");
        }

        VehImportData vehImportData = VehImportData.builder()
                .id(1L)
                .batchNum(batchNum)
                .type("PRODUCE")
                .version("1.0")
                .handle(false)
                .description(longDescription.toString())
                .createTime(LocalDateTime.now())
                .build();

        StringBuilder longError = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            longError.append("C");
        }

        when(vehSecurityConstantRepository.selectByVin(vin)).thenReturn(null);
        when(vehSecurityConstantRepository.insert(any(VehSecurityConstant.class))).thenReturn(1);
        // Make update throw to simulate failure in KMS/HSM call
        when(vehSecurityConstantRepository.update(any(VehSecurityConstant.class)))
                .thenThrow(new RuntimeException(longError.toString()));
        when(vehImportDataRepository.selectByBatchNum(batchNum)).thenReturn(vehImportData);
        when(vehImportDataRepository.update(any(VehImportData.class))).thenReturn(1);

        // When & Then - should not throw
        assertDoesNotThrow(() -> vehicleSecurityPresetAppService.preset(vin, batchNum));

        // Verify vehImportData was updated with truncated description
        verify(vehImportDataRepository).update(argThat(entity -> {
            assertNotNull(entity.getDescription());
            assertTrue(entity.getDescription().length() <= 500);
            assertTrue(entity.getDescription().endsWith("..."));
            return true;
        }));
    }

    @Test
    @DisplayName("500字符的description不应被截断")
    void testTruncateDescriptionAtExactly500Characters() {
        // Given
        String vin = "TEST_VIN_BOUNDARY_1";
        String batchNum = "BATCH_BOUNDARY_1";

        StringBuilder exactly500 = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            exactly500.append("A");
        }
        assertEquals(500, exactly500.length());

        when(vehSecurityConstantRepository.selectByVin(vin)).thenReturn(null);
        when(vehSecurityConstantRepository.insert(any(VehSecurityConstant.class))).thenReturn(1);
        // First update throws (in preset try block), second update succeeds (in handlePresetFailure)
        when(vehSecurityConstantRepository.update(any(VehSecurityConstant.class)))
                .thenThrow(new RuntimeException(exactly500.toString()))
                .thenReturn(1);
        when(vehImportDataRepository.selectByBatchNum(batchNum)).thenReturn(null);

        // When & Then
        assertDoesNotThrow(() -> vehicleSecurityPresetAppService.preset(vin, batchNum));

        // Verify 2 calls: first in preset (throws), second in handlePresetFailure (succeeds)
        ArgumentCaptor<VehSecurityConstant> updateCaptor = ArgumentCaptor.forClass(VehSecurityConstant.class);
        verify(vehSecurityConstantRepository, times(2)).update(updateCaptor.capture());

        // Check the second call (in handlePresetFailure) has correct failReason
        List<VehSecurityConstant> allUpdates = updateCaptor.getAllValues();
        VehSecurityConstant handlePresetFailureUpdate = allUpdates.get(1);
        assertEquals(exactly500.toString(), handlePresetFailureUpdate.getFailReason());
        assertEquals(500, handlePresetFailureUpdate.getFailReason().length());
        assertFalse(handlePresetFailureUpdate.getFailReason().endsWith("..."));
    }

    @Test
    @DisplayName("501字符的description应被截断为500字符并以...结尾")
    void testTruncateDescriptionAt501Characters() {
        // Given
        String vin = "TEST_VIN_BOUNDARY_2";
        String batchNum = "BATCH_BOUNDARY_2";

        StringBuilder exactly501 = new StringBuilder();
        for (int i = 0; i < 501; i++) {
            exactly501.append("B");
        }
        assertEquals(501, exactly501.length());

        when(vehSecurityConstantRepository.selectByVin(vin)).thenReturn(null);
        when(vehSecurityConstantRepository.insert(any(VehSecurityConstant.class))).thenReturn(1);
        // First update throws (in preset try block), second update succeeds (in handlePresetFailure)
        when(vehSecurityConstantRepository.update(any(VehSecurityConstant.class)))
                .thenThrow(new RuntimeException(exactly501.toString()))
                .thenReturn(1);
        when(vehImportDataRepository.selectByBatchNum(batchNum)).thenReturn(null);

        // When & Then
        assertDoesNotThrow(() -> vehicleSecurityPresetAppService.preset(vin, batchNum));

        // Verify 2 calls: first in preset (throws), second in handlePresetFailure (succeeds)
        ArgumentCaptor<VehSecurityConstant> updateCaptor = ArgumentCaptor.forClass(VehSecurityConstant.class);
        verify(vehSecurityConstantRepository, times(2)).update(updateCaptor.capture());

        // Check the second call (in handlePresetFailure) has truncated failReason
        List<VehSecurityConstant> allUpdates = updateCaptor.getAllValues();
        VehSecurityConstant handlePresetFailureUpdate = allUpdates.get(1);
        assertEquals(500, handlePresetFailureUpdate.getFailReason().length());
        assertTrue(handlePresetFailureUpdate.getFailReason().endsWith("..."));
        assertEquals(497, handlePresetFailureUpdate.getFailReason().length() - 3); // 497 B's + "..."
    }

    @Test
    @DisplayName("503字符的description应被截断为500字符并以...结尾")
    void testTruncateDescriptionAt503Characters() {
        // Given
        String vin = "TEST_VIN_BOUNDARY_3";
        String batchNum = "BATCH_BOUNDARY_3";

        StringBuilder exactly503 = new StringBuilder();
        for (int i = 0; i < 503; i++) {
            exactly503.append("C");
        }
        assertEquals(503, exactly503.length());

        when(vehSecurityConstantRepository.selectByVin(vin)).thenReturn(null);
        when(vehSecurityConstantRepository.insert(any(VehSecurityConstant.class))).thenReturn(1);
        // First update throws (in preset try block), second update succeeds (in handlePresetFailure)
        when(vehSecurityConstantRepository.update(any(VehSecurityConstant.class)))
                .thenThrow(new RuntimeException(exactly503.toString()))
                .thenReturn(1);
        when(vehImportDataRepository.selectByBatchNum(batchNum)).thenReturn(null);

        // When & Then
        assertDoesNotThrow(() -> vehicleSecurityPresetAppService.preset(vin, batchNum));

        // Verify 2 calls: first in preset (throws), second in handlePresetFailure (succeeds)
        ArgumentCaptor<VehSecurityConstant> updateCaptor = ArgumentCaptor.forClass(VehSecurityConstant.class);
        verify(vehSecurityConstantRepository, times(2)).update(updateCaptor.capture());

        // Check the second call (in handlePresetFailure) has truncated failReason
        List<VehSecurityConstant> allUpdates = updateCaptor.getAllValues();
        VehSecurityConstant handlePresetFailureUpdate = allUpdates.get(1);
        assertEquals(500, handlePresetFailureUpdate.getFailReason().length());
        assertTrue(handlePresetFailureUpdate.getFailReason().endsWith("..."));
    }

    @Test
    @DisplayName("veh_import_data不存在时不抛异常")
    void testPresetFailureWhenImportDataNotFound() {
        // Given
        String vin = "TEST_VIN_007";
        String batchNum = "BATCH_007";

        when(vehSecurityConstantRepository.selectByVin(vin)).thenReturn(null);
        when(vehSecurityConstantRepository.insert(any(VehSecurityConstant.class))).thenReturn(1);
        // Make update throw to simulate failure in KMS/HSM call
        when(vehSecurityConstantRepository.update(any(VehSecurityConstant.class)))
                .thenThrow(new RuntimeException("KMS/HSM unavailable"));
        when(vehImportDataRepository.selectByBatchNum(batchNum)).thenReturn(null);

        // When & Then - should not throw
        assertDoesNotThrow(() -> vehicleSecurityPresetAppService.preset(vin, batchNum));

        verify(vehImportDataRepository).selectByBatchNum(batchNum);
        verify(vehImportDataRepository, never()).update(any());
    }
}
