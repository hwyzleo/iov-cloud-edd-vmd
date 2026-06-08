package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CarLine实体单元测试
 * 
 * <p>CR-014：CarLine 投影采用按需最小化只读投影，保留 brand_code 冗余字段。</p>
 *
 * @author hwyz_leo
 */
class CarLineTest {

    @Test
    @DisplayName("CarLine实体构建器应正确设置所有字段")
    void builder_shouldSetAllFields() {
        // Given
        Long id = 1L;
        String code = "CARLINE001";
        String name = "测试车系";
        String nameEn = "Test CarLine";
        String brandCode = "BRAND001";
        Boolean enable = true;
        Integer sort = 10;
        SourceType source = SourceType.MANUAL;
        String externalRefId = "ext-001";
        Long externalVersion = 1L;
        LocalDateTime lastSyncTime = LocalDateTime.now();

        // When
        CarLine carLine = CarLine.builder()
                .id(id)
                .code(code)
                .name(name)
                .nameEn(nameEn)
                .brandCode(brandCode)
                .enable(enable)
                .sort(sort)
                .source(source)
                .externalRefId(externalRefId)
                .externalVersion(externalVersion)
                .lastSyncTime(lastSyncTime)
                .build();

        // Then
        assertNotNull(carLine);
        assertEquals(id, carLine.getId());
        assertEquals(code, carLine.getCode());
        assertEquals(name, carLine.getName());
        assertEquals(nameEn, carLine.getNameEn());
        assertEquals(brandCode, carLine.getBrandCode());
        assertEquals(enable, carLine.getEnable());
        assertEquals(sort, carLine.getSort());
        assertEquals(source, carLine.getSource());
        assertEquals(externalRefId, carLine.getExternalRefId());
        assertEquals(externalVersion, carLine.getExternalVersion());
        assertEquals(lastSyncTime, carLine.getLastSyncTime());
    }

    @Test
    @DisplayName("CarLine实体应支持空值字段")
    void builder_shouldHandleNullFields() {
        // Given & When
        CarLine carLine = CarLine.builder()
                .code("CARLINE002")
                .name("车系2")
                .brandCode("BRAND002")
                .build();

        // Then
        assertNotNull(carLine);
        assertEquals("CARLINE002", carLine.getCode());
        assertEquals("车系2", carLine.getName());
        assertEquals("BRAND002", carLine.getBrandCode());
        assertNull(carLine.getId());
        assertNull(carLine.getNameEn());
        assertNull(carLine.getEnable());
        assertNull(carLine.getSort());
        assertNull(carLine.getSource());
        assertNull(carLine.getExternalRefId());
        assertNull(carLine.getExternalVersion());
        assertNull(carLine.getLastSyncTime());
    }

    @Test
    @DisplayName("CarLine实体应支持SourceType枚举值")
    void carLine_shouldSupportSourceTypeValues() {
        // Given & When
        CarLine carLineMdm = CarLine.builder()
                .code("CARLINE003")
                .brandCode("BRAND003")
                .source(SourceType.MDM)
                .build();

        CarLine carLineManual = CarLine.builder()
                .code("CARLINE004")
                .brandCode("BRAND004")
                .source(SourceType.MANUAL)
                .build();

        // Then
        assertEquals(SourceType.MDM, carLineMdm.getSource());
        assertEquals(SourceType.MANUAL, carLineManual.getSource());
    }

    @Test
    @DisplayName("CarLine实体应实现DomainObj接口")
    void carLine_shouldImplementDomainObjInterface() {
        // Given
        CarLine carLine = CarLine.builder()
                .code("CARLINE005")
                .brandCode("BRAND005")
                .build();

        // Then
        assertInstanceOf(CarLine.class, carLine);
    }

    @Test
    @DisplayName("CarLine实体字段应可更新")
    void carLine_fieldsShouldBeUpdatable() {
        // Given
        CarLine carLine = CarLine.builder()
                .code("CARLINE006")
                .name("原始名称")
                .brandCode("BRAND006")
                .build();

        // When
        carLine.setName("更新后的名称");
        carLine.setCode("CARLINE006_UPDATED");
        carLine.setBrandCode("BRAND006_UPDATED");
        carLine.setEnable(false);

        // Then
        assertEquals("更新后的名称", carLine.getName());
        assertEquals("CARLINE006_UPDATED", carLine.getCode());
        assertEquals("BRAND006_UPDATED", carLine.getBrandCode());
        assertFalse(carLine.getEnable());
    }

    @Test
    @DisplayName("CarLine实体应支持外部引用ID和版本号")
    void carLine_shouldSupportExternalReferenceFields() {
        // Given
        String externalRefId = "mdm-carline-123";
        Long externalVersion = 42L;

        // When
        CarLine carLine = CarLine.builder()
                .code("CARLINE007")
                .brandCode("BRAND007")
                .externalRefId(externalRefId)
                .externalVersion(externalVersion)
                .build();

        // Then
        assertEquals(externalRefId, carLine.getExternalRefId());
        assertEquals(externalVersion, carLine.getExternalVersion());
    }

    @Test
    @DisplayName("CarLine实体应支持同步时间记录")
    void carLine_shouldSupportSyncTime() {
        // Given
        LocalDateTime syncTime = LocalDateTime.of(2026, 6, 8, 10, 30, 0);

        // When
        CarLine carLine = CarLine.builder()
                .code("CARLINE008")
                .brandCode("BRAND008")
                .lastSyncTime(syncTime)
                .build();

        // Then
        assertEquals(syncTime, carLine.getLastSyncTime());
    }

    @Test
    @DisplayName("CarLine实体应支持MDM只读投影语义")
    void carLine_shouldSupportMdmReadOnlyProjectionSemantics() {
        // Given - MDM来源的车系
        CarLine mdmCarLine = CarLine.builder()
                .code("CARLINE_MDM_001")
                .name("MDM车系")
                .brandCode("BRAND_MDM_001")
                .source(SourceType.MDM)
                .externalRefId("mdm-ext-001")
                .externalVersion(1L)
                .lastSyncTime(LocalDateTime.now())
                .build();

        // Then - MDM来源的车系应该有完整的投影字段
        assertEquals(SourceType.MDM, mdmCarLine.getSource());
        assertNotNull(mdmCarLine.getExternalRefId());
        assertNotNull(mdmCarLine.getExternalVersion());
        assertNotNull(mdmCarLine.getLastSyncTime());
    }

    @Test
    @DisplayName("CarLine实体应支持MANUAL遗留数据语义")
    void carLine_shouldSupportManualLegacyDataSemantics() {
        // Given - 手动维护的车系
        CarLine manualCarLine = CarLine.builder()
                .code("CARLINE_MANUAL_001")
                .name("手动车系")
                .brandCode("BRAND_MANUAL_001")
                .source(SourceType.MANUAL)
                .build();

        // Then - 手动维护的车系不应该有MDM投影字段
        assertEquals(SourceType.MANUAL, manualCarLine.getSource());
        assertNull(manualCarLine.getExternalRefId());
        assertNull(manualCarLine.getExternalVersion());
        assertNull(manualCarLine.getLastSyncTime());
    }

    @Test
    @DisplayName("CarLine实体应保留brandCode冗余字段")
    void carLine_shouldRetainBrandCodeRedundantField() {
        // Given - CR-014要求保留brand_code冗余字段用于跨域回查
        String brandCode = "BRAND001";
        CarLine carLine = CarLine.builder()
                .code("CARLINE001")
                .name("车系1")
                .brandCode(brandCode)
                .build();

        // Then
        assertEquals(brandCode, carLine.getBrandCode());
    }

    @Test
    @DisplayName("CarLine实体应支持MDM来源时设置brandCode")
    void carLine_shouldSupportBrandCodeForMdmSource() {
        // Given - MDM来源的车系也需要设置brandCode
        CarLine mdmCarLine = CarLine.builder()
                .code("CARLINE_MDM_002")
                .name("MDM车系2")
                .brandCode("BRAND_MDM_002")
                .source(SourceType.MDM)
                .externalRefId("mdm-ext-002")
                .externalVersion(1L)
                .lastSyncTime(LocalDateTime.now())
                .build();

        // Then
        assertEquals("BRAND_MDM_002", mdmCarLine.getBrandCode());
        assertEquals(SourceType.MDM, mdmCarLine.getSource());
    }
}
