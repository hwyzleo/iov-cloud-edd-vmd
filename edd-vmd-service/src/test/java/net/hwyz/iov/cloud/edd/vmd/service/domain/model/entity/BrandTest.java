package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Brand实体单元测试
 *
 * @author hwyz_leo
 */
class BrandTest {

    @Test
    @DisplayName("Brand实体构建器应正确设置所有字段")
    void builder_shouldSetAllFields() {
        // Given
        Long id = 1L;
        String code = "BRAND001";
        String name = "测试品牌";
        String nameEn = "Test Brand";
        Boolean enable = true;
        Integer sort = 10;
        SourceType source = SourceType.MANUAL;
        String externalRefId = "ext-001";
        Long externalVersion = 1L;
        LocalDateTime lastSyncTime = LocalDateTime.now();

        // When
        Brand brand = Brand.builder()
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
        assertNotNull(brand);
        assertEquals(id, brand.getId());
        assertEquals(code, brand.getCode());
        assertEquals(name, brand.getName());
        assertEquals(nameEn, brand.getNameEn());
        assertEquals(enable, brand.getEnable());
        assertEquals(sort, brand.getSort());
        assertEquals(source, brand.getSource());
        assertEquals(externalRefId, brand.getExternalRefId());
        assertEquals(externalVersion, brand.getExternalVersion());
        assertEquals(lastSyncTime, brand.getLastSyncTime());
    }

    @Test
    @DisplayName("Brand实体应支持空值字段")
    void builder_shouldHandleNullFields() {
        // Given & When
        Brand brand = Brand.builder()
                .code("BRAND002")
                .name("品牌2")
                .build();

        // Then
        assertNotNull(brand);
        assertEquals("BRAND002", brand.getCode());
        assertEquals("品牌2", brand.getName());
        assertNull(brand.getId());
        assertNull(brand.getNameEn());
        assertNull(brand.getEnable());
        assertNull(brand.getSort());
        assertNull(brand.getSource());
        assertNull(brand.getExternalRefId());
        assertNull(brand.getExternalVersion());
        assertNull(brand.getLastSyncTime());
    }

    @Test
    @DisplayName("Brand实体应支持SourceType枚举值")
    void brand_shouldSupportSourceTypeValues() {
        // Given & When
        Brand brandMdm = Brand.builder()
                .code("BRAND003")
                .source(SourceType.MDM)
                .build();

        Brand brandManual = Brand.builder()
                .code("BRAND004")
                .source(SourceType.MANUAL)
                .build();

        // Then
        assertEquals(SourceType.MDM, brandMdm.getSource());
        assertEquals(SourceType.MANUAL, brandManual.getSource());
    }

    @Test
    @DisplayName("Brand实体应实现DomainObj接口")
    void brand_shouldImplementDomainObjInterface() {
        // Given
        Brand brand = Brand.builder()
                .code("BRAND005")
                .build();

        // Then
        assertInstanceOf(Brand.class, brand);
    }

    @Test
    @DisplayName("Brand实体字段应可更新")
    void brand_fieldsShouldBeUpdatable() {
        // Given
        Brand brand = Brand.builder()
                .code("BRAND006")
                .name("原始名称")
                .build();

        // When
        brand.setName("更新后的名称");
        brand.setCode("BRAND006_UPDATED");
        brand.setEnable(false);

        // Then
        assertEquals("更新后的名称", brand.getName());
        assertEquals("BRAND006_UPDATED", brand.getCode());
        assertFalse(brand.getEnable());
    }

    @Test
    @DisplayName("Brand实体应支持外部引用ID和版本号")
    void brand_shouldSupportExternalReferenceFields() {
        // Given
        String externalRefId = "mdm-brand-123";
        Long externalVersion = 42L;

        // When
        Brand brand = Brand.builder()
                .code("BRAND007")
                .externalRefId(externalRefId)
                .externalVersion(externalVersion)
                .build();

        // Then
        assertEquals(externalRefId, brand.getExternalRefId());
        assertEquals(externalVersion, brand.getExternalVersion());
    }

    @Test
    @DisplayName("Brand实体应支持同步时间记录")
    void brand_shouldSupportSyncTime() {
        // Given
        LocalDateTime syncTime = LocalDateTime.of(2026, 6, 5, 10, 30, 0);

        // When
        Brand brand = Brand.builder()
                .code("BRAND008")
                .lastSyncTime(syncTime)
                .build();

        // Then
        assertEquals(syncTime, brand.getLastSyncTime());
    }

    @Test
    @DisplayName("Brand实体应支持MDM只读投影语义")
    void brand_shouldSupportMdmReadOnlyProjectionSemantics() {
        // Given - MDM来源的品牌
        Brand mdmBrand = Brand.builder()
                .code("BRAND_MDM_001")
                .name("MDM品牌")
                .source(SourceType.MDM)
                .externalRefId("mdm-ext-001")
                .externalVersion(1L)
                .lastSyncTime(LocalDateTime.now())
                .build();

        // Then - MDM来源的品牌应该有完整的投影字段
        assertEquals(SourceType.MDM, mdmBrand.getSource());
        assertNotNull(mdmBrand.getExternalRefId());
        assertNotNull(mdmBrand.getExternalVersion());
        assertNotNull(mdmBrand.getLastSyncTime());
    }

    @Test
    @DisplayName("Brand实体应支持MANUAL遗留数据语义")
    void brand_shouldSupportManualLegacyDataSemantics() {
        // Given - 手动维护的品牌
        Brand manualBrand = Brand.builder()
                .code("BRAND_MANUAL_001")
                .name("手动品牌")
                .source(SourceType.MANUAL)
                .build();

        // Then - 手动维护的品牌不应该有MDM投影字段
        assertEquals(SourceType.MANUAL, manualBrand.getSource());
        assertNull(manualBrand.getExternalRefId());
        assertNull(manualBrand.getExternalVersion());
        assertNull(manualBrand.getLastSyncTime());
    }
}
