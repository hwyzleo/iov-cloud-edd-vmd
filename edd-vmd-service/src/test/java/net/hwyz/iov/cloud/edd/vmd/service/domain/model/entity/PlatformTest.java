package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Platform实体单元测试
 *
 * @author hwyz_leo
 */
class PlatformTest {

    @Test
    @DisplayName("Platform实体构建器应正确设置所有字段")
    void builder_shouldSetAllFields() {
        // Given
        Long id = 1L;
        String code = "PLATFORM001";
        String name = "测试平台";
        String nameEn = "Test Platform";
        Boolean enable = true;
        Integer sort = 10;
        SourceType source = SourceType.MANUAL;
        String externalRefId = "ext-001";
        Long externalVersion = 1L;
        LocalDateTime lastSyncTime = LocalDateTime.now();

        // When
        Platform platform = Platform.builder()
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
        assertNotNull(platform);
        assertEquals(id, platform.getId());
        assertEquals(code, platform.getCode());
        assertEquals(name, platform.getName());
        assertEquals(nameEn, platform.getNameEn());
        assertEquals(enable, platform.getEnable());
        assertEquals(sort, platform.getSort());
        assertEquals(source, platform.getSource());
        assertEquals(externalRefId, platform.getExternalRefId());
        assertEquals(externalVersion, platform.getExternalVersion());
        assertEquals(lastSyncTime, platform.getLastSyncTime());
    }

    @Test
    @DisplayName("Platform实体应支持空值字段")
    void builder_shouldHandleNullFields() {
        // Given & When
        Platform platform = Platform.builder()
                .code("PLATFORM002")
                .name("平台2")
                .build();

        // Then
        assertNotNull(platform);
        assertEquals("PLATFORM002", platform.getCode());
        assertEquals("平台2", platform.getName());
        assertNull(platform.getId());
        assertNull(platform.getNameEn());
        assertNull(platform.getEnable());
        assertNull(platform.getSort());
        assertNull(platform.getSource());
        assertNull(platform.getExternalRefId());
        assertNull(platform.getExternalVersion());
        assertNull(platform.getLastSyncTime());
    }

    @Test
    @DisplayName("Platform实体应支持SourceType枚举值")
    void platform_shouldSupportSourceTypeValues() {
        // Given & When
        Platform platformMdm = Platform.builder()
                .code("PLATFORM003")
                .source(SourceType.MDM)
                .build();

        Platform platformManual = Platform.builder()
                .code("PLATFORM004")
                .source(SourceType.MANUAL)
                .build();

        // Then
        assertEquals(SourceType.MDM, platformMdm.getSource());
        assertEquals(SourceType.MANUAL, platformManual.getSource());
    }

    @Test
    @DisplayName("Platform实体应实现DomainObj接口")
    void platform_shouldImplementDomainObjInterface() {
        // Given
        Platform platform = Platform.builder()
                .code("PLATFORM005")
                .build();

        // Then
        assertInstanceOf(Platform.class, platform);
    }

    @Test
    @DisplayName("Platform实体字段应可更新")
    void platform_fieldsShouldBeUpdatable() {
        // Given
        Platform platform = Platform.builder()
                .code("PLATFORM006")
                .name("原始名称")
                .build();

        // When
        platform.setName("更新后的名称");
        platform.setCode("PLATFORM006_UPDATED");
        platform.setEnable(false);

        // Then
        assertEquals("更新后的名称", platform.getName());
        assertEquals("PLATFORM006_UPDATED", platform.getCode());
        assertFalse(platform.getEnable());
    }

    @Test
    @DisplayName("Platform实体应支持外部引用ID和版本号")
    void platform_shouldSupportExternalReferenceFields() {
        // Given
        String externalRefId = "mdm-platform-123";
        Long externalVersion = 42L;

        // When
        Platform platform = Platform.builder()
                .code("PLATFORM007")
                .externalRefId(externalRefId)
                .externalVersion(externalVersion)
                .build();

        // Then
        assertEquals(externalRefId, platform.getExternalRefId());
        assertEquals(externalVersion, platform.getExternalVersion());
    }

    @Test
    @DisplayName("Platform实体应支持同步时间记录")
    void platform_shouldSupportSyncTime() {
        // Given
        LocalDateTime syncTime = LocalDateTime.of(2026, 6, 5, 10, 30, 0);

        // When
        Platform platform = Platform.builder()
                .code("PLATFORM008")
                .lastSyncTime(syncTime)
                .build();

        // Then
        assertEquals(syncTime, platform.getLastSyncTime());
    }

    @Test
    @DisplayName("Platform实体应支持MDM只读投影语义")
    void platform_shouldSupportMdmReadOnlyProjectionSemantics() {
        // Given - MDM来源的平台
        Platform mdmPlatform = Platform.builder()
                .code("PLATFORM_MDM_001")
                .name("MDM平台")
                .source(SourceType.MDM)
                .externalRefId("mdm-ext-001")
                .externalVersion(1L)
                .lastSyncTime(LocalDateTime.now())
                .build();

        // Then - MDM来源的平台应该有完整的投影字段
        assertEquals(SourceType.MDM, mdmPlatform.getSource());
        assertNotNull(mdmPlatform.getExternalRefId());
        assertNotNull(mdmPlatform.getExternalVersion());
        assertNotNull(mdmPlatform.getLastSyncTime());
    }

    @Test
    @DisplayName("Platform实体应支持MANUAL遗留数据语义")
    void platform_shouldSupportManualLegacyDataSemantics() {
        // Given - 手动维护的平台
        Platform manualPlatform = Platform.builder()
                .code("PLATFORM_MANUAL_001")
                .name("手动平台")
                .source(SourceType.MANUAL)
                .build();

        // Then - 手动维护的平台不应该有MDM投影字段
        assertEquals(SourceType.MANUAL, manualPlatform.getSource());
        assertNull(manualPlatform.getExternalRefId());
        assertNull(manualPlatform.getExternalVersion());
        assertNull(manualPlatform.getLastSyncTime());
    }
}
