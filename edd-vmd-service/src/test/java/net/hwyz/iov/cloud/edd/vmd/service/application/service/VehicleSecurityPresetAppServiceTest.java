package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehImportData;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehSecurityConstant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SecurityConstantState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehImportDataRepository;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehSecurityConstantRepository;
import net.hwyz.iov.cloud.framework.security.crypto.KeyProvisioningTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.model.BizType;
import net.hwyz.iov.cloud.framework.security.crypto.model.ProvisioningResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
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

    @Mock
    private KeyProvisioningTemplate keyProvisioningTemplate;

    private VehicleSecurityPresetAppService vehicleSecurityPresetAppService;

    @BeforeEach
    void setUp() {
        vehicleSecurityPresetAppService = new VehicleSecurityPresetAppService(
                vehSecurityConstantRepository, vehImportDataRepository, keyProvisioningTemplate);
    }

    private ProvisioningResult mockResult() {
        ProvisioningResult result = new ProvisioningResult();
        result.setKmsKeyRef("secoc-master:vin:TEST_VIN");
        result.setKeySpec("256-bit");
        result.setProvider("Vault-Transit");
        result.setAlgorithm("HMAC-SHA256");
        result.setKcv(new byte[]{1, 2, 3, 4});
        result.setWrappedMaterial(null);
        return result;
    }

    @Test
    @DisplayName("应成功预置新车辆安全常量")
    void testPresetSuccessForNewVehicle() {
        String vin = "TEST_VIN_001";
        String batchNum = "BATCH_001";

        when(vehSecurityConstantRepository.selectByVin(vin)).thenReturn(null);
        when(vehSecurityConstantRepository.insert(any(VehSecurityConstant.class))).thenReturn(1);
        when(vehSecurityConstantRepository.update(any(VehSecurityConstant.class))).thenReturn(1);
        when(keyProvisioningTemplate.deriveByVin(vin, BizType.V2C_COMM_ROOT)).thenReturn(mockResult());

        vehicleSecurityPresetAppService.preset(vin, batchNum);

        verify(vehSecurityConstantRepository).selectByVin(vin);
        verify(vehSecurityConstantRepository).insert(any(VehSecurityConstant.class));
        verify(vehSecurityConstantRepository).update(argThat(entity -> {
            assertEquals(SecurityConstantState.PRESET, entity.getPresetState());
            assertEquals("secoc-master:vin:TEST_VIN", entity.getKmsKeyRef());
            assertEquals("256-bit", entity.getKeySpec());
            assertEquals("Vault-Transit", entity.getKmsProvider());
            assertEquals("HMAC-SHA256", entity.getAlgorithm());
            assertEquals("01020304", entity.getKcv());
            assertNotNull(entity.getGenTime());
            assertNotNull(entity.getLastAttemptTime());
            return true;
        }));
        verify(keyProvisioningTemplate).deriveByVin(vin, BizType.V2C_COMM_ROOT);
    }

    @Test
    @DisplayName("应跳过已预置的车辆安全常量（幂等检查）")
    void testPresetSkipAlreadyPresetVehicle() {
        String vin = "TEST_VIN_002";
        String batchNum = "BATCH_002";

        VehSecurityConstant existing = VehSecurityConstant.builder()
                .id(1L)
                .vin(vin)
                .batchNum(batchNum)
                .presetState(SecurityConstantState.PRESET)
                .constantType("ROOT")
                .createTime(LocalDateTime.now())
                .build();

        when(vehSecurityConstantRepository.selectByVin(vin)).thenReturn(existing);

        vehicleSecurityPresetAppService.preset(vin, batchNum);

        verify(vehSecurityConstantRepository).selectByVin(vin);
        verify(vehSecurityConstantRepository, never()).insert(any());
        verify(vehSecurityConstantRepository, never()).update(any());
        verify(keyProvisioningTemplate, never()).deriveByVin(any(), any());
    }

    @Test
    @DisplayName("应更新已存在但未预置的车辆安全常量")
    void testPresetUpdateExistingNonPresetVehicle() {
        String vin = "TEST_VIN_003";
        String batchNum = "BATCH_003";

        VehSecurityConstant existing = VehSecurityConstant.builder()
                .id(1L)
                .vin(vin)
                .batchNum("OLD_BATCH")
                .presetState(SecurityConstantState.FAILED)
                .constantType("ROOT")
                .createTime(LocalDateTime.now())
                .build();

        when(vehSecurityConstantRepository.selectByVin(vin)).thenReturn(existing);
        when(vehSecurityConstantRepository.update(any(VehSecurityConstant.class))).thenReturn(1);
        when(keyProvisioningTemplate.deriveByVin(vin, BizType.V2C_COMM_ROOT)).thenReturn(mockResult());

        vehicleSecurityPresetAppService.preset(vin, batchNum);

        verify(vehSecurityConstantRepository).selectByVin(vin);
        verify(vehSecurityConstantRepository, never()).insert(any());
        verify(vehSecurityConstantRepository).update(argThat(entity -> {
            assertEquals(SecurityConstantState.PRESET, entity.getPresetState());
            assertEquals(batchNum, entity.getBatchNum());
            return true;
        }));
    }

    @Test
    @DisplayName("应正确设置createTime和constantType为ROOT")
    void testPresetSetsCreateTimeAndConstantType() {
        String vin = "TEST_VIN_004";
        String batchNum = "BATCH_004";

        when(vehSecurityConstantRepository.selectByVin(vin)).thenReturn(null);
        when(vehSecurityConstantRepository.insert(any(VehSecurityConstant.class))).thenReturn(1);
        when(vehSecurityConstantRepository.update(any(VehSecurityConstant.class))).thenReturn(1);
        when(keyProvisioningTemplate.deriveByVin(vin, BizType.V2C_COMM_ROOT)).thenReturn(mockResult());

        vehicleSecurityPresetAppService.preset(vin, batchNum);

        verify(vehSecurityConstantRepository).insert(argThat(entity -> {
            assertEquals(vin, entity.getVin());
            assertEquals(batchNum, entity.getBatchNum());
            assertEquals("ROOT", entity.getConstantType());
            assertNotNull(entity.getCreateTime());
            return true;
        }));
    }

    @Test
    @DisplayName("KMS调用失败时应置FAILED并截断failReason")
    void testPresetFailureTruncatesFailReason() {
        String vin = "TEST_VIN_005";
        String batchNum = "BATCH_005";

        StringBuilder longError = new StringBuilder();
        for (int i = 0; i < 600; i++) {
            longError.append("A");
        }

        when(vehSecurityConstantRepository.selectByVin(vin)).thenReturn(null);
        when(vehSecurityConstantRepository.insert(any(VehSecurityConstant.class))).thenReturn(1);
        when(keyProvisioningTemplate.deriveByVin(vin, BizType.V2C_COMM_ROOT))
                .thenThrow(new RuntimeException(longError.toString()));
        when(vehImportDataRepository.selectByBatchNum(batchNum)).thenReturn(null);
        when(vehSecurityConstantRepository.update(any(VehSecurityConstant.class))).thenReturn(1);

        assertDoesNotThrow(() -> vehicleSecurityPresetAppService.preset(vin, batchNum));

        ArgumentCaptor<VehSecurityConstant> updateCaptor = ArgumentCaptor.forClass(VehSecurityConstant.class);
        verify(vehSecurityConstantRepository, times(1)).update(updateCaptor.capture());

        VehSecurityConstant failureUpdate = updateCaptor.getValue();
        assertEquals(SecurityConstantState.FAILED, failureUpdate.getPresetState());
        assertNotNull(failureUpdate.getFailReason());
        assertTrue(failureUpdate.getFailReason().length() <= 500);
        assertTrue(failureUpdate.getFailReason().endsWith("..."));
    }

    @Test
    @DisplayName("KMS调用失败时应写回veh_import_data.description")
    void testPresetFailureWritesBackDescription() {
        String vin = "TEST_VIN_006";
        String batchNum = "BATCH_006";

        VehImportData vehImportData = VehImportData.builder()
                .id(1L)
                .batchNum(batchNum)
                .type("PRODUCE")
                .version("1.0")
                .handle(false)
                .description("existing desc")
                .createTime(LocalDateTime.now())
                .build();

        when(vehSecurityConstantRepository.selectByVin(vin)).thenReturn(null);
        when(vehSecurityConstantRepository.insert(any(VehSecurityConstant.class))).thenReturn(1);
        when(keyProvisioningTemplate.deriveByVin(vin, BizType.V2C_COMM_ROOT))
                .thenThrow(new RuntimeException("KMS unavailable"));
        when(vehImportDataRepository.selectByBatchNum(batchNum)).thenReturn(vehImportData);
        when(vehImportDataRepository.update(any(VehImportData.class))).thenReturn(1);
        when(vehSecurityConstantRepository.update(any(VehSecurityConstant.class))).thenReturn(1);

        assertDoesNotThrow(() -> vehicleSecurityPresetAppService.preset(vin, batchNum));

        verify(vehImportDataRepository).update(argThat(entity -> {
            assertNotNull(entity.getDescription());
            assertTrue(entity.getDescription().contains("安全常量预置失败"));
            assertTrue(entity.getDescription().length() <= 500);
            return true;
        }));
    }

    @Test
    @DisplayName("EOL补发的PRODUCE事件不触发安全预置")
    void testEolProduceEventSkipped() {
        String vin = "TEST_VIN_007";
        String batchNum = "EOL-BATCH_007";

        vehicleSecurityPresetAppService.preset(vin, batchNum);

        verify(vehSecurityConstantRepository, never()).selectByVin(any());
        verify(keyProvisioningTemplate, never()).deriveByVin(any(), any());
    }

    @Test
    @DisplayName("veh_import_data不存在时不抛异常")
    void testPresetFailureWhenImportDataNotFound() {
        String vin = "TEST_VIN_008";
        String batchNum = "BATCH_008";

        when(vehSecurityConstantRepository.selectByVin(vin)).thenReturn(null);
        when(vehSecurityConstantRepository.insert(any(VehSecurityConstant.class))).thenReturn(1);
        when(keyProvisioningTemplate.deriveByVin(vin, BizType.V2C_COMM_ROOT))
                .thenThrow(new RuntimeException("KMS unavailable"));
        when(vehImportDataRepository.selectByBatchNum(batchNum)).thenReturn(null);
        when(vehSecurityConstantRepository.update(any(VehSecurityConstant.class))).thenReturn(1);

        assertDoesNotThrow(() -> vehicleSecurityPresetAppService.preset(vin, batchNum));

        verify(vehImportDataRepository).selectByBatchNum(batchNum);
        verify(vehImportDataRepository, never()).update(any());
    }
}
