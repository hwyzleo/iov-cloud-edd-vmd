package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.VehSecurityConstant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SecurityConstantState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VehSecurityConstantRepositoryImpl集成测试
 *
 * @author hwyz_leo
 * @since 2026-06-17
 */
@SpringBootTest
@Transactional
class VehSecurityConstantRepositoryImplTest {

    @Autowired
    private VehSecurityConstantRepositoryImpl vehSecurityConstantRepository;

    @Test
    void testInsertAndSelectByVin() {
        // Given
        String vin = "TEST1234567890123";
        VehSecurityConstant entity = VehSecurityConstant.builder()
                .vin(vin)
                .batchNum("BATCH001")
                .presetState(SecurityConstantState.PENDING)
                .build();
        entity.init();

        // When
        int rows = vehSecurityConstantRepository.insert(entity);
        VehSecurityConstant result = vehSecurityConstantRepository.selectByVin(vin);

        // Then
        assertEquals(1, rows);
        assertNotNull(result);
        assertEquals(vin, result.getVin());
        assertEquals(SecurityConstantState.PENDING, result.getPresetState());
    }

    @Test
    void testUpdate() {
        // Given
        String vin = "TEST1234567890123";
        VehSecurityConstant entity = VehSecurityConstant.builder()
                .vin(vin)
                .batchNum("BATCH001")
                .presetState(SecurityConstantState.PENDING)
                .build();
        entity.init();
        vehSecurityConstantRepository.insert(entity);

        VehSecurityConstant inserted = vehSecurityConstantRepository.selectByVin(vin);
        inserted.setPresetState(SecurityConstantState.PRESET);
        inserted.setKeyHandle("test_key_handle");
        inserted.setCipherBlob("test_cipher_blob");

        // When
        int rows = vehSecurityConstantRepository.update(inserted);
        VehSecurityConstant result = vehSecurityConstantRepository.selectByVin(vin);

        // Then
        assertEquals(1, rows);
        assertNotNull(result);
        assertEquals(SecurityConstantState.PRESET, result.getPresetState());
        assertEquals("test_key_handle", result.getKeyHandle());
        assertEquals("test_cipher_blob", result.getCipherBlob());
    }

    @Test
    void testCountByVin() {
        // Given
        String vin = "TEST1234567890123";
        VehSecurityConstant entity = VehSecurityConstant.builder()
                .vin(vin)
                .batchNum("BATCH001")
                .presetState(SecurityConstantState.PENDING)
                .build();
        entity.init();
        vehSecurityConstantRepository.insert(entity);

        // When
        long count = vehSecurityConstantRepository.countByVin(vin);

        // Then
        assertEquals(1, count);
    }
}
