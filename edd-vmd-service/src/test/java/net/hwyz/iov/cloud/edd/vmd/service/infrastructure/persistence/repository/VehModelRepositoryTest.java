package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Model;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehModelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VehModelRepository集成测试
 *
 * @author hwyz_leo
 */
@Rollback
class VehModelRepositoryTest extends BaseTest {

    @Autowired
    private VehModelRepository vehModelRepository;

    private String generateUniqueCode() {
        return "MODEL_" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    @DisplayName("应成功插入车型记录")
    void insert_shouldSuccessfullyInsertModel() {
        // Given
        String code = generateUniqueCode();
        Model model = Model.builder()
                .code(code)
                .name("测试车型")
                .nameEn("Test Model")
                .platformCode("PLATFORM001")
                .carLineCode("CARLINE001")
                .enable(true)
                .sort(1)
                .source(SourceType.MANUAL)
                .build();

        // When
        int result = vehModelRepository.insert(model);

        // Then
        assertEquals(1, result);
        assertNotNull(model.getId());
    }

    @Test
    @DisplayName("应成功根据ID查询车型")
    void selectById_shouldReturnModelWhenExists() {
        // Given
        String code = generateUniqueCode();
        Model model = Model.builder()
                .code(code)
                .name("测试车型2")
                .platformCode("PLATFORM001")
                .carLineCode("CARLINE001")
                .enable(true)
                .sort(2)
                .source(SourceType.MANUAL)
                .build();
        vehModelRepository.insert(model);

        // When
        Model result = vehModelRepository.selectById(model.getId());

        // Then
        assertNotNull(result);
        assertEquals(model.getId(), result.getId());
        assertEquals(code, result.getCode());
        assertEquals("测试车型2", result.getName());
        assertEquals("PLATFORM001", result.getPlatformCode());
        assertEquals("CARLINE001", result.getCarLineCode());
    }

    @Test
    @DisplayName("应成功根据代码查询车型")
    void selectByCode_shouldReturnModelWhenCodeExists() {
        // Given
        String code = generateUniqueCode();
        Model model = Model.builder()
                .code(code)
                .name("测试车型3")
                .platformCode("PLATFORM001")
                .carLineCode("CARLINE001")
                .enable(true)
                .sort(3)
                .source(SourceType.MANUAL)
                .build();
        vehModelRepository.insert(model);

        // When
        Model result = vehModelRepository.selectByCode(code);

        // Then
        assertNotNull(result);
        assertEquals(code, result.getCode());
    }

    @Test
    @DisplayName("应成功更新车型信息")
    void update_shouldSuccessfullyUpdateModel() {
        // Given
        String code = generateUniqueCode();
        Model model = Model.builder()
                .code(code)
                .name("原始名称")
                .platformCode("PLATFORM001")
                .carLineCode("CARLINE001")
                .enable(true)
                .sort(4)
                .source(SourceType.MANUAL)
                .build();
        vehModelRepository.insert(model);

        // When
        model.setName("更新后的名称");
        model.setSort(10);
        model.setPlatformCode("PLATFORM002");
        int result = vehModelRepository.update(model);

        // Then
        assertEquals(1, result);
        Model updatedModel = vehModelRepository.selectById(model.getId());
        assertEquals("更新后的名称", updatedModel.getName());
        assertEquals(10, updatedModel.getSort());
        assertEquals("PLATFORM002", updatedModel.getPlatformCode());
    }

    @Test
    @DisplayName("应成功批量删除车型")
    void batchPhysicalDelete_shouldSuccessfullyDeleteModels() {
        // Given
        String code1 = generateUniqueCode();
        String code2 = generateUniqueCode();
        Model model1 = Model.builder()
                .code(code1)
                .name("车型5")
                .platformCode("PLATFORM001")
                .carLineCode("CARLINE001")
                .enable(true)
                .sort(5)
                .source(SourceType.MANUAL)
                .build();
        Model model2 = Model.builder()
                .code(code2)
                .name("车型6")
                .platformCode("PLATFORM001")
                .carLineCode("CARLINE001")
                .enable(true)
                .sort(6)
                .source(SourceType.MANUAL)
                .build();
        vehModelRepository.insert(model1);
        vehModelRepository.insert(model2);

        Long[] ids = {model1.getId(), model2.getId()};

        // When
        int result = vehModelRepository.batchPhysicalDelete(ids);

        // Then
        assertEquals(2, result);
        assertNull(vehModelRepository.selectById(model1.getId()));
        assertNull(vehModelRepository.selectById(model2.getId()));
    }

    @Test
    @DisplayName("应成功根据条件查询车型列表")
    void selectByMap_shouldReturnModelsMatchingCriteria() {
        // Given
        String code1 = generateUniqueCode();
        String code2 = generateUniqueCode();
        Model model1 = Model.builder()
                .code(code1)
                .name("测试车型7")
                .platformCode("PLATFORM001")
                .carLineCode("CARLINE001")
                .enable(true)
                .sort(7)
                .source(SourceType.MANUAL)
                .build();
        Model model2 = Model.builder()
                .code(code2)
                .name("其他车型")
                .platformCode("PLATFORM002")
                .carLineCode("CARLINE002")
                .enable(true)
                .sort(8)
                .source(SourceType.MANUAL)
                .build();
        vehModelRepository.insert(model1);
        vehModelRepository.insert(model2);

        Map<String, Object> map = new HashMap<>();
        map.put("platformCode", "PLATFORM001");

        // When
        List<Model> result = vehModelRepository.selectByMap(map);

        // Then
        assertNotNull(result);
        assertTrue(result.stream().anyMatch(m -> code1.equals(m.getCode())));
    }

    @Test
    @DisplayName("应成功统计车型数量")
    void countByMap_shouldReturnCorrectCount() {
        // Given
        String code = generateUniqueCode();
        Model model = Model.builder()
                .code(code)
                .name("车型9")
                .platformCode("PLATFORM001")
                .carLineCode("CARLINE001")
                .enable(true)
                .sort(9)
                .source(SourceType.MANUAL)
                .build();
        vehModelRepository.insert(model);

        Map<String, Object> map = new HashMap<>();
        map.put("code", code);

        // When
        int result = vehModelRepository.countByMap(map);

        // Then
        assertEquals(1, result);
    }

    @Test
    @DisplayName("应成功根据外部引用ID查询车型")
    void selectByExternalRefId_shouldReturnModelWhenExternalRefIdExists() {
        // Given
        String code = generateUniqueCode();
        String externalRefId = "ext-ref-" + UUID.randomUUID().toString().substring(0, 8);
        Model model = Model.builder()
                .code(code)
                .name("车型10")
                .platformCode("PLATFORM001")
                .carLineCode("CARLINE001")
                .enable(true)
                .sort(10)
                .source(SourceType.MDM)
                .externalRefId(externalRefId)
                .externalVersion(1L)
                .lastSyncTime(LocalDateTime.now())
                .build();
        vehModelRepository.insert(model);

        // When
        Model result = vehModelRepository.selectByExternalRefId(externalRefId);

        // Then
        assertNotNull(result);
        assertEquals(externalRefId, result.getExternalRefId());
        assertEquals(SourceType.MDM, result.getSource());
    }

    @Test
    @DisplayName("应成功统计指定来源的车型数量")
    void countBySource_shouldReturnCountForSource() {
        // Given
        String code1 = generateUniqueCode();
        String code2 = generateUniqueCode();
        Model modelMdm = Model.builder()
                .code(code1)
                .name("MDM车型")
                .platformCode("PLATFORM001")
                .carLineCode("CARLINE001")
                .enable(true)
                .sort(11)
                .source(SourceType.MDM)
                .externalRefId("ext-" + UUID.randomUUID().toString().substring(0, 8))
                .externalVersion(1L)
                .build();
        Model modelManual = Model.builder()
                .code(code2)
                .name("手动车型")
                .platformCode("PLATFORM001")
                .carLineCode("CARLINE001")
                .enable(true)
                .sort(12)
                .source(SourceType.MANUAL)
                .build();
        vehModelRepository.insert(modelMdm);
        vehModelRepository.insert(modelManual);

        // When
        long mdmCount = vehModelRepository.countBySource(SourceType.MDM);
        long manualCount = vehModelRepository.countBySource(SourceType.MANUAL);

        // Then
        assertTrue(mdmCount >= 1);
        assertTrue(manualCount >= 1);
    }

    @Test
    @DisplayName("应成功更新MDM投影字段")
    void update_shouldSuccessfullyUpdateMdmProjectionFields() {
        // Given
        String code = generateUniqueCode();
        Model model = Model.builder()
                .code(code)
                .name("车型13")
                .platformCode("PLATFORM001")
                .carLineCode("CARLINE001")
                .enable(true)
                .sort(13)
                .source(SourceType.MANUAL)
                .build();
        vehModelRepository.insert(model);

        // When
        model.setSource(SourceType.MDM);
        model.setExternalRefId("ext-" + UUID.randomUUID().toString().substring(0, 8));
        model.setExternalVersion(2L);
        model.setLastSyncTime(LocalDateTime.now());
        int result = vehModelRepository.update(model);

        // Then
        assertEquals(1, result);
        Model updatedModel = vehModelRepository.selectById(model.getId());
        assertEquals(SourceType.MDM, updatedModel.getSource());
        assertNotNull(updatedModel.getExternalRefId());
        assertEquals(2L, updatedModel.getExternalVersion());
        assertNotNull(updatedModel.getLastSyncTime());
    }

    @Test
    @DisplayName("应成功插入MDM来源车型并设置投影字段")
    void insert_shouldSuccessfullyInsertMdmModelWithProjectionFields() {
        // Given
        String code = generateUniqueCode();
        Model model = Model.builder()
                .code(code)
                .name("MDM车型14")
                .platformCode("PLATFORM001")
                .carLineCode("CARLINE001")
                .enable(true)
                .sort(14)
                .source(SourceType.MDM)
                .externalRefId("ext-" + UUID.randomUUID().toString().substring(0, 8))
                .externalVersion(1L)
                .lastSyncTime(LocalDateTime.now())
                .build();

        // When
        int result = vehModelRepository.insert(model);

        // Then
        assertEquals(1, result);
        assertNotNull(model.getId());

        Model insertedModel = vehModelRepository.selectById(model.getId());
        assertEquals(SourceType.MDM, insertedModel.getSource());
        assertNotNull(insertedModel.getExternalRefId());
        assertEquals(1L, insertedModel.getExternalVersion());
        assertNotNull(insertedModel.getLastSyncTime());
    }

    @Test
    @DisplayName("应成功根据平台代码和车系代码查询车型列表")
    void selectByPlatformCodeAndCarLineCode_shouldReturnModelsForPlatformAndCarLine() {
        // Given
        String platformCode = "PLATFORM_SELECT_" + UUID.randomUUID().toString().substring(0, 4);
        String carLineCode = "CARLINE_SELECT_" + UUID.randomUUID().toString().substring(0, 4);

        Model model1 = Model.builder()
                .code(generateUniqueCode())
                .name("车型15")
                .platformCode(platformCode)
                .carLineCode(carLineCode)
                .enable(true)
                .sort(15)
                .source(SourceType.MANUAL)
                .build();
        Model model2 = Model.builder()
                .code(generateUniqueCode())
                .name("车型16")
                .platformCode(platformCode)
                .carLineCode(carLineCode)
                .enable(true)
                .sort(16)
                .source(SourceType.MDM)
                .externalRefId("ext-" + UUID.randomUUID().toString().substring(0, 8))
                .externalVersion(1L)
                .build();

        vehModelRepository.insert(model1);
        vehModelRepository.insert(model2);

        Map<String, Object> map = new HashMap<>();
        map.put("platformCode", platformCode);
        map.put("carLineCode", carLineCode);

        // When
        List<Model> result = vehModelRepository.selectByMap(map);

        // Then
        assertNotNull(result);
        assertTrue(result.stream().anyMatch(m -> model1.getCode().equals(m.getCode())));
        assertTrue(result.stream().anyMatch(m -> model2.getCode().equals(m.getCode())));
    }

    @Test
    @DisplayName("应成功更新车型的平台和车系关联字段")
    void update_shouldSuccessfullyUpdatePlatformAndCarLineFields() {
        // Given
        String code = generateUniqueCode();
        Model model = Model.builder()
                .code(code)
                .name("车型17")
                .platformCode("PLATFORM_ORIGINAL")
                .carLineCode("CARLINE_ORIGINAL")
                .enable(true)
                .sort(17)
                .source(SourceType.MANUAL)
                .build();
        vehModelRepository.insert(model);

        // When
        model.setPlatformCode("PLATFORM_UPDATED");
        model.setCarLineCode("CARLINE_UPDATED");
        int result = vehModelRepository.update(model);

        // Then
        assertEquals(1, result);
        Model updatedModel = vehModelRepository.selectById(model.getId());
        assertEquals("PLATFORM_UPDATED", updatedModel.getPlatformCode());
        assertEquals("CARLINE_UPDATED", updatedModel.getCarLineCode());
    }
}
