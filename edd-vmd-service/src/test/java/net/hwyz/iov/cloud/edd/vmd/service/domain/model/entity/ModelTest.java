package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Model实体单元测试
 *
 * @author hwyz_leo
 */
class ModelTest {

    @Test
    @DisplayName("Model实体构建器应正确设置所有字段")
    void builder_shouldSetAllFields() {
        // Given
        Long id = 1L;
        String platformCode = "PLATFORM001";
        String carLineCode = "CARLINE001";
        String code = "MODEL001";
        String name = "测试车型";
        String nameEn = "Test Model";
        Boolean enable = true;
        Integer sort = 10;
        SourceType source = SourceType.MANUAL;
        String externalRefId = "ext-001";
        Long externalVersion = 1L;
        LocalDateTime lastSyncTime = LocalDateTime.now();

        // When
        Model model = Model.builder()
                .id(id)
                .platformCode(platformCode)
                .carLineCode(carLineCode)
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
        assertNotNull(model);
        assertEquals(id, model.getId());
        assertEquals(platformCode, model.getPlatformCode());
        assertEquals(carLineCode, model.getCarLineCode());
        assertEquals(code, model.getCode());
        assertEquals(name, model.getName());
        assertEquals(nameEn, model.getNameEn());
        assertEquals(enable, model.getEnable());
        assertEquals(sort, model.getSort());
        assertEquals(source, model.getSource());
        assertEquals(externalRefId, model.getExternalRefId());
        assertEquals(externalVersion, model.getExternalVersion());
        assertEquals(lastSyncTime, model.getLastSyncTime());
    }

    @Test
    @DisplayName("Model实体应支持空值字段")
    void builder_shouldHandleNullFields() {
        // Given & When
        Model model = Model.builder()
                .code("MODEL002")
                .name("车型2")
                .build();

        // Then
        assertNotNull(model);
        assertEquals("MODEL002", model.getCode());
        assertEquals("车型2", model.getName());
        assertNull(model.getId());
        assertNull(model.getPlatformCode());
        assertNull(model.getCarLineCode());
        assertNull(model.getNameEn());
        assertNull(model.getEnable());
        assertNull(model.getSort());
        assertNull(model.getSource());
        assertNull(model.getExternalRefId());
        assertNull(model.getExternalVersion());
        assertNull(model.getLastSyncTime());
    }

    @Test
    @DisplayName("Model实体应支持SourceType枚举值")
    void model_shouldSupportSourceTypeValues() {
        // Given & When
        Model modelMdm = Model.builder()
                .code("MODEL003")
                .source(SourceType.MDM)
                .build();

        Model modelManual = Model.builder()
                .code("MODEL004")
                .source(SourceType.MANUAL)
                .build();

        // Then
        assertEquals(SourceType.MDM, modelMdm.getSource());
        assertEquals(SourceType.MANUAL, modelManual.getSource());
    }

    @Test
    @DisplayName("Model实体应实现DomainObj接口")
    void model_shouldImplementDomainObjInterface() {
        // Given
        Model model = Model.builder()
                .code("MODEL005")
                .build();

        // Then
        assertInstanceOf(Model.class, model);
    }

    @Test
    @DisplayName("Model实体字段应可更新")
    void model_fieldsShouldBeUpdatable() {
        // Given
        Model model = Model.builder()
                .code("MODEL006")
                .name("原始名称")
                .build();

        // When
        model.setName("更新后的名称");
        model.setCode("MODEL006_UPDATED");
        model.setEnable(false);
        model.setPlatformCode("PLATFORM002");
        model.setCarLineCode("CARLINE002");

        // Then
        assertEquals("更新后的名称", model.getName());
        assertEquals("MODEL006_UPDATED", model.getCode());
        assertFalse(model.getEnable());
        assertEquals("PLATFORM002", model.getPlatformCode());
        assertEquals("CARLINE002", model.getCarLineCode());
    }

    @Test
    @DisplayName("Model实体应支持外部引用ID和版本号")
    void model_shouldSupportExternalReferenceFields() {
        // Given
        String externalRefId = "mdm-model-123";
        Long externalVersion = 42L;

        // When
        Model model = Model.builder()
                .code("MODEL007")
                .externalRefId(externalRefId)
                .externalVersion(externalVersion)
                .build();

        // Then
        assertEquals(externalRefId, model.getExternalRefId());
        assertEquals(externalVersion, model.getExternalVersion());
    }

    @Test
    @DisplayName("Model实体应支持同步时间记录")
    void model_shouldSupportSyncTime() {
        // Given
        LocalDateTime syncTime = LocalDateTime.of(2026, 6, 5, 10, 30, 0);

        // When
        Model model = Model.builder()
                .code("MODEL008")
                .lastSyncTime(syncTime)
                .build();

        // Then
        assertEquals(syncTime, model.getLastSyncTime());
    }

    @Test
    @DisplayName("Model实体应支持MDM只读投影语义")
    void model_shouldSupportMdmReadOnlyProjectionSemantics() {
        // Given - MDM来源的车型
        Model mdmModel = Model.builder()
                .code("MODEL_MDM_001")
                .name("MDM车型")
                .platformCode("PLATFORM001")
                .carLineCode("CARLINE001")
                .source(SourceType.MDM)
                .externalRefId("mdm-ext-001")
                .externalVersion(1L)
                .lastSyncTime(LocalDateTime.now())
                .build();

        // Then - MDM来源的车型应该有完整的投影字段
        assertEquals(SourceType.MDM, mdmModel.getSource());
        assertNotNull(mdmModel.getExternalRefId());
        assertNotNull(mdmModel.getExternalVersion());
        assertNotNull(mdmModel.getLastSyncTime());
        assertEquals("PLATFORM001", mdmModel.getPlatformCode());
        assertEquals("CARLINE001", mdmModel.getCarLineCode());
    }

    @Test
    @DisplayName("Model实体应支持MANUAL遗留数据语义")
    void model_shouldSupportManualLegacyDataSemantics() {
        // Given - 手动维护的车型
        Model manualModel = Model.builder()
                .code("MODEL_MANUAL_001")
                .name("手动车型")
                .platformCode("PLATFORM001")
                .carLineCode("CARLINE001")
                .source(SourceType.MANUAL)
                .build();

        // Then - 手动维护的车型不应该有MDM投影字段
        assertEquals(SourceType.MANUAL, manualModel.getSource());
        assertNull(manualModel.getExternalRefId());
        assertNull(manualModel.getExternalVersion());
        assertNull(manualModel.getLastSyncTime());
    }

    @Test
    @DisplayName("Model实体应支持平台和车系关联字段")
    void model_shouldSupportPlatformAndCarLineAssociation() {
        // Given
        String platformCode = "PLATFORM001";
        String carLineCode = "CARLINE001";

        // When
        Model model = Model.builder()
                .code("MODEL009")
                .platformCode(platformCode)
                .carLineCode(carLineCode)
                .build();

        // Then
        assertEquals(platformCode, model.getPlatformCode());
        assertEquals(carLineCode, model.getCarLineCode());
    }
}
