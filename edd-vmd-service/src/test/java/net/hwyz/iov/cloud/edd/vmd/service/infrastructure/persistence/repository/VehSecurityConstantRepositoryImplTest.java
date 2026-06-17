package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehSecurityConstant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SecurityConstantState;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.VehSecurityConstantMapper;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.po.VehSecurityConstantPo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * VehSecurityConstantRepository 单元测试
 * <p>
 * VMD-DSN-CR-028: 车辆安全常量仓储测试
 *
 * @author hwyz_leo
 * @since 2026-06-17
 */
@ExtendWith(MockitoExtension.class)
class VehSecurityConstantRepositoryImplTest {

    @Mock
    private VehSecurityConstantMapper vehSecurityConstantMapper;

    private VehSecurityConstantRepositoryImpl vehSecurityConstantRepository;

    @BeforeEach
    void setUp() {
        vehSecurityConstantRepository = new VehSecurityConstantRepositoryImpl(vehSecurityConstantMapper);
    }

    private VehSecurityConstantPo buildPo(String vin) {
        return VehSecurityConstantPo.builder()
                .id(1L)
                .vin(vin)
                .batchNum("BATCH_001")
                .presetState("PENDING")
                .keyHandle("key-handle-001")
                .cipherBlob("cipher-blob-data")
                .constantType("SECURITY_KEY")
                .createTime(new Date())
                .build();
    }

    private VehSecurityConstant buildDomain(String vin) {
        return VehSecurityConstant.builder()
                .id(1L)
                .vin(vin)
                .batchNum("BATCH_001")
                .presetState(SecurityConstantState.PENDING)
                .keyHandle("key-handle-001")
                .cipherBlob("cipher-blob-data")
                .constantType("SECURITY_KEY")
                .createTime(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("应成功根据车架号查询车辆安全常量")
    void selectByVin_shouldReturnVehSecurityConstantWhenExists() {
        // Given
        String vin = "TEST_VIN_001";
        VehSecurityConstantPo po = buildPo(vin);

        when(vehSecurityConstantMapper.selectPoByVin(vin)).thenReturn(po);

        // When
        VehSecurityConstant result = vehSecurityConstantRepository.selectByVin(vin);

        // Then
        assertNotNull(result);
        assertEquals(vin, result.getVin());
        assertEquals(1L, result.getId());
        assertEquals("BATCH_001", result.getBatchNum());
        assertEquals(SecurityConstantState.PENDING, result.getPresetState());
        verify(vehSecurityConstantMapper).selectPoByVin(vin);
    }

    @Test
    @DisplayName("应返回null当车架号不存在时")
    void selectByVin_shouldReturnNullWhenVinNotFound() {
        // Given
        String vin = "NONEXISTENT_VIN";

        when(vehSecurityConstantMapper.selectPoByVin(vin)).thenReturn(null);

        // When
        VehSecurityConstant result = vehSecurityConstantRepository.selectByVin(vin);

        // Then
        assertNull(result);
        verify(vehSecurityConstantMapper).selectPoByVin(vin);
    }

    @Test
    @DisplayName("应成功插入车辆安全常量")
    void insert_shouldSuccessfullyInsertVehSecurityConstant() {
        // Given
        VehSecurityConstant vehSecurityConstant = buildDomain("TEST_VIN_002");

        when(vehSecurityConstantMapper.insertPo(any(VehSecurityConstantPo.class))).thenReturn(1);

        // When
        int result = vehSecurityConstantRepository.insert(vehSecurityConstant);

        // Then
        assertEquals(1, result);
        verify(vehSecurityConstantMapper).insertPo(any(VehSecurityConstantPo.class));
    }

    @Test
    @DisplayName("应成功更新车辆安全常量")
    void update_shouldSuccessfullyUpdateVehSecurityConstant() {
        // Given
        VehSecurityConstant vehSecurityConstant = buildDomain("TEST_VIN_003");
        vehSecurityConstant.setId(1L);
        vehSecurityConstant.setPresetState(SecurityConstantState.PRESET);

        when(vehSecurityConstantMapper.updatePo(any(VehSecurityConstantPo.class))).thenReturn(1);

        // When
        int result = vehSecurityConstantRepository.update(vehSecurityConstant);

        // Then
        assertEquals(1, result);
        verify(vehSecurityConstantMapper).updatePo(any(VehSecurityConstantPo.class));
    }

    @Test
    @DisplayName("应正确返回车架号对应的数量")
    void countByVin_shouldReturnCorrectCount() {
        // Given
        String vin = "TEST_VIN_004";

        when(vehSecurityConstantMapper.countPoByVin(vin)).thenReturn(1L);

        // When
        long result = vehSecurityConstantRepository.countByVin(vin);

        // Then
        assertEquals(1L, result);
        verify(vehSecurityConstantMapper).countPoByVin(vin);
    }

    @Test
    @DisplayName("应返回0当车架号不存在时")
    void countByVin_shouldReturnZeroWhenVinNotFound() {
        // Given
        String vin = "NONEXISTENT_VIN";

        when(vehSecurityConstantMapper.countPoByVin(vin)).thenReturn(0L);

        // When
        long result = vehSecurityConstantRepository.countByVin(vin);

        // Then
        assertEquals(0L, result);
        verify(vehSecurityConstantMapper).countPoByVin(vin);
    }
}
