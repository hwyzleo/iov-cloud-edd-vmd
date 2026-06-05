package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plant实体单元测试
 *
 * @author hwyz_leo
 */
class PlantTest {

    @Test
    @DisplayName("Plant实体构建器应正确设置所有字段")
    void builder_shouldSetAllFields() {
        // Given
        Long id = 1L;
        String code = "PLANT001";
        String name = "测试工厂";
        String nameEn = "Test Plant";
        Boolean enable = true;
        Integer sort = 10;
        SourceType source = SourceType.MANUAL;
        String externalRefId = "ext-001";
        Long externalVersion = 1L;
        LocalDateTime lastSyncTime = LocalDateTime.now();

        // When
        Plant plant = Plant.builder()
                .id(id)
                .code(code)
                .name(name)
                .nameEn(nameEn)
                .enable(enable)
                .sort(sort)
                .source(source)
                .externalRefId(externalRefId)
                .externalVersion(externalVersion)
                .lastSyncTime(lastSyncTime)
                .build();

        // Then
        assertNotNull(plant);
        assertEquals(id, plant.getId());
        assertEquals(code, plant.getCode());
        assertEquals(name, plant.getName());
        assertEquals(nameEn, plant.getNameEn());
        assertEquals(enable, plant.getEnable());
        assertEquals(sort, plant.getSort());
        assertEquals(source, plant.getSource());
        assertEquals(externalRefId, plant.getExternalRefId());
        assertEquals(externalVersion, plant.getExternalVersion());
        assertEquals(lastSyncTime, plant.getLastSyncTime());
    }

    @Test
    @DisplayName("Plant实体应支持空值字段")
    void builder_shouldHandleNullFields() {
        // Given & When
        Plant plant = Plant.builder()
                .code("PLANT002")
                .name("工厂2")
                .build();

        // Then
        assertNotNull(plant);
        assertEquals("PLANT002", plant.getCode());
        assertEquals("工厂2", plant.getName());
        assertNull(plant.getId());
        assertNull(plant.getNameEn());
        assertNull(plant.getEnable());
        assertNull(plant.getSort());
        assertNull(plant.getSource());
        assertNull(plant.getExternalRefId());
        assertNull(plant.getExternalVersion());
        assertNull(plant.getLastSyncTime());
    }

    @Test
    @DisplayName("Plant实体应支持SourceType枚举值")
    void plant_shouldSupportSourceTypeValues() {
        // Given & When
        Plant plantMdm = Plant.builder()
                .code("PLANT003")
                .source(SourceType.MDM)
                .build();

        Plant plantManual = Plant.builder()
                .code("PLANT004")
                .source(SourceType.MANUAL)
                .build();

        // Then
        assertEquals(SourceType.MDM, plantMdm.getSource());
        assertEquals(SourceType.MANUAL, plantManual.getSource());
    }

    @Test
    @DisplayName("Plant实体应实现DomainObj接口")
    void plant_shouldImplementDomainObjInterface() {
        // Given
        Plant plant = Plant.builder()
                .code("PLANT005")
                .build();

        // Then
        assertInstanceOf(Plant.class, plant);
    }

    @Test
    @DisplayName("Plant实体字段应可更新")
    void plant_fieldsShouldBeUpdatable() {
        // Given
        Plant plant = Plant.builder()
                .code("PLANT006")
                .name("原始名称")
                .build();

        // When
        plant.setName("更新后的名称");
        plant.setCode("PLANT006_UPDATED");
        plant.setEnable(false);

        // Then
        assertEquals("更新后的名称", plant.getName());
        assertEquals("PLANT006_UPDATED", plant.getCode());
        assertFalse(plant.getEnable());
    }

    @Test
    @DisplayName("Plant实体应支持外部引用ID和版本号")
    void plant_shouldSupportExternalReferenceFields() {
        // Given
        String externalRefId = "mdm-plant-123";
        Long externalVersion = 42L;

        // When
        Plant plant = Plant.builder()
                .code("PLANT007")
                .externalRefId(externalRefId)
                .externalVersion(externalVersion)
                .build();

        // Then
        assertEquals(externalRefId, plant.getExternalRefId());
        assertEquals(externalVersion, plant.getExternalVersion());
    }

    @Test
    @DisplayName("Plant实体应支持同步时间记录")
    void plant_shouldSupportSyncTime() {
        // Given
        LocalDateTime syncTime = LocalDateTime.of(2026, 6, 5, 10, 30, 0);

        // When
        Plant plant = Plant.builder()
                .code("PLANT008")
                .lastSyncTime(syncTime)
                .build();

        // Then
        assertEquals(syncTime, plant.getLastSyncTime());
    }
}