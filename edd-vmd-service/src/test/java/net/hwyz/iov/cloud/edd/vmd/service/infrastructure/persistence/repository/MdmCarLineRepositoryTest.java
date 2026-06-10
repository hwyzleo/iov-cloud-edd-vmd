package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.CarLine;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmCarLineRepository;
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
 * MdmCarLineRepository集成测试
 * 
 * <p>CR-014：CarLine 投影采用按需最小化只读投影，保留 brand_code 冗余字段。</p>
 *
 * @author hwyz_leo
 */
@Rollback
class MdmCarLineRepositoryTest extends BaseTest {

    @Autowired
    private MdmCarLineRepository mdmCarLineRepository;

    @Test
    @DisplayName("应成功插入车系记录")
    void insert_shouldSuccessfullyInsertCarLine() {
        // Given
        CarLine carLine = CarLine.builder()
                .code("TEST_CARLINE_001")
                .name("测试车系")
                .nameEn("Test CarLine")
                .brandCode("BRAND001")
                .enable(true)
                .sort(1)
                .source(SourceType.MANUAL)
                .build();

        // When
        int result = mdmCarLineRepository.insert(carLine);

        // Then
        assertEquals(1, result);
        assertNotNull(carLine.getId());
    }

    @Test
    @DisplayName("应成功根据ID查询车系")
    void selectById_shouldReturnCarLineWhenExists() {
        // Given
        CarLine carLine = CarLine.builder()
                .code("TEST_CARLINE_002")
                .name("测试车系2")
                .brandCode("BRAND002")
                .enable(true)
                .sort(2)
                .build();
        mdmCarLineRepository.insert(carLine);

        // When
        CarLine result = mdmCarLineRepository.selectById(carLine.getId());

        // Then
        assertNotNull(result);
        assertEquals(carLine.getId(), result.getId());
        assertEquals("TEST_CARLINE_002", result.getCode());
        assertEquals("测试车系2", result.getName());
        assertEquals("BRAND002", result.getBrandCode());
    }

    @Test
    @DisplayName("应成功根据代码查询车系")
    void selectByCode_shouldReturnCarLineWhenCodeExists() {
        // Given
        String code = "TEST_CARLINE_003";
        CarLine carLine = CarLine.builder()
                .code(code)
                .name("测试车系3")
                .brandCode("BRAND003")
                .enable(true)
                .sort(3)
                .build();
        mdmCarLineRepository.insert(carLine);

        // When
        CarLine result = mdmCarLineRepository.selectByCode(code);

        // Then
        assertNotNull(result);
        assertEquals(code, result.getCode());
    }

    @Test
    @DisplayName("应成功更新车系信息")
    void update_shouldSuccessfullyUpdateCarLine() {
        // Given
        CarLine carLine = CarLine.builder()
                .code("TEST_CARLINE_004")
                .name("原始名称")
                .brandCode("BRAND004")
                .enable(true)
                .sort(4)
                .build();
        mdmCarLineRepository.insert(carLine);

        // When
        carLine.setName("更新后的名称");
        carLine.setBrandCode("BRAND004_UPDATED");
        carLine.setSort(10);
        int result = mdmCarLineRepository.update(carLine);

        // Then
        assertEquals(1, result);
        CarLine updatedCarLine = mdmCarLineRepository.selectById(carLine.getId());
        assertEquals("更新后的名称", updatedCarLine.getName());
        assertEquals("BRAND004_UPDATED", updatedCarLine.getBrandCode());
        assertEquals(10, updatedCarLine.getSort());
    }

    @Test
    @DisplayName("应成功批量删除车系")
    void batchPhysicalDelete_shouldSuccessfullyDeleteCarLines() {
        // Given
        CarLine carLine1 = CarLine.builder()
                .code("TEST_CARLINE_005")
                .name("车系5")
                .brandCode("BRAND005")
                .enable(true)
                .sort(5)
                .build();
        CarLine carLine2 = CarLine.builder()
                .code("TEST_CARLINE_006")
                .name("车系6")
                .brandCode("BRAND006")
                .enable(true)
                .sort(6)
                .build();
        mdmCarLineRepository.insert(carLine1);
        mdmCarLineRepository.insert(carLine2);

        Long[] ids = {carLine1.getId(), carLine2.getId()};

        // When
        int result = mdmCarLineRepository.batchPhysicalDelete(ids);

        // Then
        assertEquals(2, result);
        assertNull(mdmCarLineRepository.selectById(carLine1.getId()));
        assertNull(mdmCarLineRepository.selectById(carLine2.getId()));
    }

    @Test
    @DisplayName("应成功根据条件查询车系列表")
    void selectByMap_shouldReturnCarLinesMatchingCriteria() {
        // Given
        CarLine carLine1 = CarLine.builder()
                .code("TEST_CARLINE_007")
                .name("测试车系7")
                .brandCode("BRAND007")
                .enable(true)
                .sort(7)
                .build();
        CarLine carLine2 = CarLine.builder()
                .code("TEST_CARLINE_008")
                .name("其他车系")
                .brandCode("BRAND008")
                .enable(true)
                .sort(8)
                .build();
        mdmCarLineRepository.insert(carLine1);
        mdmCarLineRepository.insert(carLine2);

        Map<String, Object> map = new HashMap<>();
        map.put("name", "测试");

        // When
        List<CarLine> result = mdmCarLineRepository.selectByMap(map);

        // Then
        assertNotNull(result);
        assertTrue(result.stream().anyMatch(c -> "TEST_CARLINE_007".equals(c.getCode())));
    }

    @Test
    @DisplayName("应成功统计车系数量")
    void countByMap_shouldReturnCorrectCount() {
        // Given
        CarLine carLine = CarLine.builder()
                .code("TEST_CARLINE_009")
                .name("车系9")
                .brandCode("BRAND009")
                .enable(true)
                .sort(9)
                .build();
        mdmCarLineRepository.insert(carLine);

        Map<String, Object> map = new HashMap<>();
        map.put("code", "TEST_CARLINE_009");

        // When
        int result = mdmCarLineRepository.countByMap(map);

        // Then
        assertEquals(1, result);
    }

    @Test
    @DisplayName("应成功根据外部引用ID查询车系")
    void selectByExternalRefId_shouldReturnCarLineWhenExternalRefIdExists() {
        // Given
        String externalRefId = "ext-ref-001";
        CarLine carLine = CarLine.builder()
                .code("TEST_CARLINE_010")
                .name("车系10")
                .brandCode("BRAND010")
                .enable(true)
                .sort(10)
                .source(SourceType.MDM)
                .externalRefId(externalRefId)
                .externalVersion(1L)
                .lastSyncTime(LocalDateTime.now())
                .build();
        mdmCarLineRepository.insert(carLine);

        // When
        CarLine result = mdmCarLineRepository.selectByExternalRefId(externalRefId);

        // Then
        assertNotNull(result);
        assertEquals(externalRefId, result.getExternalRefId());
        assertEquals(SourceType.MDM, result.getSource());
    }

    @Test
    @DisplayName("应成功统计指定来源的车系数量")
    void countBySource_shouldReturnCountForSource() {
        // Given
        CarLine carLineMdm = CarLine.builder()
                .code("TEST_CARLINE_011")
                .name("MDM车系")
                .brandCode("BRAND011")
                .enable(true)
                .sort(11)
                .source(SourceType.MDM)
                .externalRefId("ext-011")
                .externalVersion(1L)
                .build();
        CarLine carLineManual = CarLine.builder()
                .code("TEST_CARLINE_012")
                .name("手动车系")
                .brandCode("BRAND012")
                .enable(true)
                .sort(12)
                .source(SourceType.MANUAL)
                .build();
        mdmCarLineRepository.insert(carLineMdm);
        mdmCarLineRepository.insert(carLineManual);

        // When
        long mdmCount = mdmCarLineRepository.countBySource(SourceType.MDM);
        long manualCount = mdmCarLineRepository.countBySource(SourceType.MANUAL);

        // Then
        assertTrue(mdmCount >= 1);
        assertTrue(manualCount >= 1);
    }

    @Test
    @DisplayName("应成功更新MDM投影字段")
    void update_shouldSuccessfullyUpdateMdmProjectionFields() {
        // Given
        CarLine carLine = CarLine.builder()
                .code("TEST_CARLINE_013")
                .name("车系13")
                .brandCode("BRAND013")
                .enable(true)
                .sort(13)
                .source(SourceType.MANUAL)
                .build();
        mdmCarLineRepository.insert(carLine);

        // When
        carLine.setSource(SourceType.MDM);
        carLine.setExternalRefId("ext-013");
        carLine.setExternalVersion(2L);
        carLine.setLastSyncTime(LocalDateTime.now());
        int result = mdmCarLineRepository.update(carLine);

        // Then
        assertEquals(1, result);
        CarLine updatedCarLine = mdmCarLineRepository.selectById(carLine.getId());
        assertEquals(SourceType.MDM, updatedCarLine.getSource());
        assertEquals("ext-013", updatedCarLine.getExternalRefId());
        assertEquals(2L, updatedCarLine.getExternalVersion());
        assertNotNull(updatedCarLine.getLastSyncTime());
    }

    @Test
    @DisplayName("应成功插入MDM来源车系并设置投影字段")
    void insert_shouldSuccessfullyInsertMdmCarLineWithProjectionFields() {
        // Given
        CarLine carLine = CarLine.builder()
                .code("TEST_CARLINE_014")
                .name("MDM车系14")
                .brandCode("BRAND014")
                .enable(true)
                .sort(14)
                .source(SourceType.MDM)
                .externalRefId("ext-014")
                .externalVersion(1L)
                .lastSyncTime(LocalDateTime.now())
                .build();

        // When
        int result = mdmCarLineRepository.insert(carLine);

        // Then
        assertEquals(1, result);
        assertNotNull(carLine.getId());

        CarLine insertedCarLine = mdmCarLineRepository.selectById(carLine.getId());
        assertEquals(SourceType.MDM, insertedCarLine.getSource());
        assertEquals("ext-014", insertedCarLine.getExternalRefId());
        assertEquals(1L, insertedCarLine.getExternalVersion());
        assertNotNull(insertedCarLine.getLastSyncTime());
    }

    @Test
    @DisplayName("应成功根据brandCode查询车系列表")
    void selectByMap_shouldReturnCarLinesByBrandCode() {
        // Given
        String brandCode = "BRAND_FILTER_001";
        CarLine carLine1 = CarLine.builder()
                .code("TEST_CARLINE_015")
                .name("车系15")
                .brandCode(brandCode)
                .enable(true)
                .sort(15)
                .build();
        CarLine carLine2 = CarLine.builder()
                .code("TEST_CARLINE_016")
                .name("车系16")
                .brandCode("BRAND_OTHER")
                .enable(true)
                .sort(16)
                .build();
        mdmCarLineRepository.insert(carLine1);
        mdmCarLineRepository.insert(carLine2);

        Map<String, Object> map = new HashMap<>();
        map.put("brandCode", brandCode);

        // When
        List<CarLine> result = mdmCarLineRepository.selectByMap(map);

        // Then
        assertNotNull(result);
        assertTrue(result.stream().allMatch(c -> brandCode.equals(c.getBrandCode())));
    }
}
