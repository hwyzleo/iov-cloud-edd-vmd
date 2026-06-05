package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Plant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehPlantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VehPlantRepository集成测试
 *
 * @author hwyz_leo
 */
@Rollback
class VehPlantRepositoryTest extends BaseTest {

    @Autowired
    private VehPlantRepository vehPlantRepository;

    @Test
    @DisplayName("应成功插入工厂记录")
    void insert_shouldSuccessfullyInsertPlant() {
        // Given
        Plant plant = Plant.builder()
                .code("TEST_PLANT_001")
                .name("测试工厂")
                .nameEn("Test Plant")
                .enable(true)
                .sort(1)
                .source(SourceType.MANUAL)
                .build();

        // When
        int result = vehPlantRepository.insert(plant);

        // Then
        assertEquals(1, result);
        assertNotNull(plant.getId());
    }

    @Test
    @DisplayName("应成功根据ID查询工厂")
    void selectById_shouldReturnPlantWhenExists() {
        // Given
        Plant plant = Plant.builder()
                .code("TEST_PLANT_002")
                .name("测试工厂2")
                .enable(true)
                .sort(2)
                .build();
        vehPlantRepository.insert(plant);

        // When
        Plant result = vehPlantRepository.selectById(plant.getId());

        // Then
        assertNotNull(result);
        assertEquals(plant.getId(), result.getId());
        assertEquals("TEST_PLANT_002", result.getCode());
        assertEquals("测试工厂2", result.getName());
    }

    @Test
    @DisplayName("应成功根据代码查询工厂")
    void selectByCode_shouldReturnPlantWhenCodeExists() {
        // Given
        String code = "TEST_PLANT_003";
        Plant plant = Plant.builder()
                .code(code)
                .name("测试工厂3")
                .enable(true)
                .sort(3)
                .build();
        vehPlantRepository.insert(plant);

        // When
        Plant result = vehPlantRepository.selectByCode(code);

        // Then
        assertNotNull(result);
        assertEquals(code, result.getCode());
    }

    @Test
    @DisplayName("应成功更新工厂信息")
    void update_shouldSuccessfullyUpdatePlant() {
        // Given
        Plant plant = Plant.builder()
                .code("TEST_PLANT_004")
                .name("原始名称")
                .enable(true)
                .sort(4)
                .build();
        vehPlantRepository.insert(plant);

        // When
        plant.setName("更新后的名称");
        plant.setSort(10);
        int result = vehPlantRepository.update(plant);

        // Then
        assertEquals(1, result);
        Plant updatedPlant = vehPlantRepository.selectById(plant.getId());
        assertEquals("更新后的名称", updatedPlant.getName());
        assertEquals(10, updatedPlant.getSort());
    }

    @Test
    @DisplayName("应成功批量删除工厂")
    void batchPhysicalDelete_shouldSuccessfullyDeletePlants() {
        // Given
        Plant plant1 = Plant.builder()
                .code("TEST_PLANT_005")
                .name("工厂5")
                .enable(true)
                .sort(5)
                .build();
        Plant plant2 = Plant.builder()
                .code("TEST_PLANT_006")
                .name("工厂6")
                .enable(true)
                .sort(6)
                .build();
        vehPlantRepository.insert(plant1);
        vehPlantRepository.insert(plant2);

        Long[] ids = {plant1.getId(), plant2.getId()};

        // When
        int result = vehPlantRepository.batchPhysicalDelete(ids);

        // Then
        assertEquals(2, result);
        assertNull(vehPlantRepository.selectById(plant1.getId()));
        assertNull(vehPlantRepository.selectById(plant2.getId()));
    }

    @Test
    @DisplayName("应成功根据条件查询工厂列表")
    void selectByMap_shouldReturnPlantsMatchingCriteria() {
        // Given
        Plant plant1 = Plant.builder()
                .code("TEST_PLANT_007")
                .name("测试工厂7")
                .enable(true)
                .sort(7)
                .build();
        Plant plant2 = Plant.builder()
                .code("TEST_PLANT_008")
                .name("其他工厂")
                .enable(true)
                .sort(8)
                .build();
        vehPlantRepository.insert(plant1);
        vehPlantRepository.insert(plant2);

        Map<String, Object> map = new HashMap<>();
        map.put("name", "测试");

        // When
        List<Plant> result = vehPlantRepository.selectByMap(map);

        // Then
        assertNotNull(result);
        assertTrue(result.stream().anyMatch(p -> "TEST_PLANT_007".equals(p.getCode())));
    }

    @Test
    @DisplayName("应成功统计工厂数量")
    void countByMap_shouldReturnCorrectCount() {
        // Given
        Plant plant = Plant.builder()
                .code("TEST_PLANT_009")
                .name("工厂9")
                .enable(true)
                .sort(9)
                .build();
        vehPlantRepository.insert(plant);

        Map<String, Object> map = new HashMap<>();
        map.put("code", "TEST_PLANT_009");

        // When
        int result = vehPlantRepository.countByMap(map);

        // Then
        assertEquals(1, result);
    }

    @Test
    @DisplayName("应成功根据外部引用ID查询工厂")
    void selectByExternalRefId_shouldReturnPlantWhenExternalRefIdExists() {
        // Given
        String externalRefId = "ext-ref-001";
        Plant plant = Plant.builder()
                .code("TEST_PLANT_010")
                .name("工厂10")
                .enable(true)
                .sort(10)
                .source(SourceType.MDM)
                .externalRefId(externalRefId)
                .externalVersion(1L)
                .lastSyncTime(LocalDateTime.now())
                .build();
        vehPlantRepository.insert(plant);

        // When
        Plant result = vehPlantRepository.selectByExternalRefId(externalRefId);

        // Then
        assertNotNull(result);
        assertEquals(externalRefId, result.getExternalRefId());
        assertEquals(SourceType.MDM, result.getSource());
    }

    @Test
    @DisplayName("应成功统计指定来源的工厂数量")
    void countBySource_shouldReturnCountForSource() {
        // Given
        Plant plantMdm = Plant.builder()
                .code("TEST_PLANT_011")
                .name("MDM工厂")
                .enable(true)
                .sort(11)
                .source(SourceType.MDM)
                .externalRefId("ext-011")
                .externalVersion(1L)
                .build();
        Plant plantManual = Plant.builder()
                .code("TEST_PLANT_012")
                .name("手动工厂")
                .enable(true)
                .sort(12)
                .source(SourceType.MANUAL)
                .build();
        vehPlantRepository.insert(plantMdm);
        vehPlantRepository.insert(plantManual);

        // When
        int mdmCount = vehPlantRepository.countBySource("MDM");
        int manualCount = vehPlantRepository.countBySource("MANUAL");

        // Then
        assertTrue(mdmCount >= 1);
        assertTrue(manualCount >= 1);
    }

    @Test
    @DisplayName("应成功更新MDM投影字段")
    void update_shouldSuccessfullyUpdateMdmProjectionFields() {
        // Given
        Plant plant = Plant.builder()
                .code("TEST_PLANT_013")
                .name("工厂13")
                .enable(true)
                .sort(13)
                .source(SourceType.MANUAL)
                .build();
        vehPlantRepository.insert(plant);

        // When
        plant.setSource(SourceType.MDM);
        plant.setExternalRefId("ext-013");
        plant.setExternalVersion(2L);
        plant.setLastSyncTime(LocalDateTime.now());
        int result = vehPlantRepository.update(plant);

        // Then
        assertEquals(1, result);
        Plant updatedPlant = vehPlantRepository.selectById(plant.getId());
        assertEquals(SourceType.MDM, updatedPlant.getSource());
        assertEquals("ext-013", updatedPlant.getExternalRefId());
        assertEquals(2L, updatedPlant.getExternalVersion());
        assertNotNull(updatedPlant.getLastSyncTime());
    }
}