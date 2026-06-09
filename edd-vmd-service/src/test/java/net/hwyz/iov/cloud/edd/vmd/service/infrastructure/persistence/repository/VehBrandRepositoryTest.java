package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Brand;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.VehBrandRepository;
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
 * VehBrandRepository集成测试
 *
 * @author hwyz_leo
 */
@Rollback
class VehBrandRepositoryTest extends BaseTest {

    @Autowired
    private VehBrandRepository vehBrandRepository;

    @Test
    @DisplayName("应成功插入品牌记录")
    void insert_shouldSuccessfullyInsertBrand() {
        // Given
        Brand brand = Brand.builder()
                .code("TEST_BRAND_001")
                .name("测试品牌")
                .nameEn("Test Brand")
                .enable(true)
                .sort(1)
                .source(SourceType.MANUAL)
                .build();

        // When
        int result = vehBrandRepository.insert(brand);

        // Then
        assertEquals(1, result);
        assertNotNull(brand.getId());
    }

    @Test
    @DisplayName("应成功根据ID查询品牌")
    void selectById_shouldReturnBrandWhenExists() {
        // Given
        Brand brand = Brand.builder()
                .code("TEST_BRAND_002")
                .name("测试品牌2")
                .enable(true)
                .sort(2)
                .source(SourceType.MANUAL)
                .build();
        vehBrandRepository.insert(brand);

        // When
        Brand result = vehBrandRepository.selectById(brand.getId());

        // Then
        assertNotNull(result);
        assertEquals(brand.getId(), result.getId());
        assertEquals("TEST_BRAND_002", result.getCode());
        assertEquals("测试品牌2", result.getName());
    }

    @Test
    @DisplayName("应成功根据代码查询品牌")
    void selectByCode_shouldReturnBrandWhenCodeExists() {
        // Given
        String code = "TEST_BRAND_003";
        Brand brand = Brand.builder()
                .code(code)
                .name("测试品牌3")
                .enable(true)
                .sort(3)
                .source(SourceType.MANUAL)
                .build();
        vehBrandRepository.insert(brand);

        // When
        Brand result = vehBrandRepository.selectByCode(code);

        // Then
        assertNotNull(result);
        assertEquals(code, result.getCode());
    }

    @Test
    @DisplayName("应成功更新品牌信息")
    void update_shouldSuccessfullyUpdateBrand() {
        // Given
        Brand brand = Brand.builder()
                .code("TEST_BRAND_004")
                .name("原始名称")
                .enable(true)
                .sort(4)
                .source(SourceType.MANUAL)
                .build();
        vehBrandRepository.insert(brand);

        // When
        brand.setName("更新后的名称");
        brand.setSort(10);
        int result = vehBrandRepository.update(brand);

        // Then
        assertEquals(1, result);
        Brand updatedBrand = vehBrandRepository.selectById(brand.getId());
        assertEquals("更新后的名称", updatedBrand.getName());
        assertEquals(10, updatedBrand.getSort());
    }

    @Test
    @DisplayName("应成功批量删除品牌")
    void batchPhysicalDelete_shouldSuccessfullyDeleteBrands() {
        // Given
        Brand brand1 = Brand.builder()
                .code("TEST_BRAND_005")
                .name("品牌5")
                .enable(true)
                .sort(5)
                .source(SourceType.MANUAL)
                .build();
        Brand brand2 = Brand.builder()
                .code("TEST_BRAND_006")
                .name("品牌6")
                .enable(true)
                .sort(6)
                .source(SourceType.MANUAL)
                .build();
        vehBrandRepository.insert(brand1);
        vehBrandRepository.insert(brand2);

        Long[] ids = {brand1.getId(), brand2.getId()};

        // When
        int result = vehBrandRepository.batchPhysicalDelete(ids);

        // Then
        assertEquals(2, result);
        assertNull(vehBrandRepository.selectById(brand1.getId()));
        assertNull(vehBrandRepository.selectById(brand2.getId()));
    }

    @Test
    @DisplayName("应成功根据条件查询品牌列表")
    void selectByMap_shouldReturnBrandsMatchingCriteria() {
        // Given
        Brand brand1 = Brand.builder()
                .code("TEST_BRAND_007")
                .name("测试品牌7")
                .enable(true)
                .sort(7)
                .source(SourceType.MANUAL)
                .build();
        Brand brand2 = Brand.builder()
                .code("TEST_BRAND_008")
                .name("其他品牌")
                .enable(true)
                .sort(8)
                .source(SourceType.MANUAL)
                .build();
        vehBrandRepository.insert(brand1);
        vehBrandRepository.insert(brand2);

        Map<String, Object> map = new HashMap<>();
        map.put("name", "测试");

        // When
        List<Brand> result = vehBrandRepository.selectByMap(map);

        // Then
        assertNotNull(result);
        assertTrue(result.stream().anyMatch(b -> "TEST_BRAND_007".equals(b.getCode())));
    }

    @Test
    @DisplayName("应成功统计品牌数量")
    void countByMap_shouldReturnCorrectCount() {
        // Given
        Brand brand = Brand.builder()
                .code("TEST_BRAND_009")
                .name("品牌9")
                .enable(true)
                .sort(9)
                .source(SourceType.MANUAL)
                .build();
        vehBrandRepository.insert(brand);

        Map<String, Object> map = new HashMap<>();
        map.put("code", "TEST_BRAND_009");

        // When
        int result = vehBrandRepository.countByMap(map);

        // Then
        assertEquals(1, result);
    }

    @Test
    @DisplayName("应成功根据外部引用ID查询品牌")
    void selectByExternalRefId_shouldReturnBrandWhenExternalRefIdExists() {
        // Given
        String externalRefId = "ext-ref-001";
        Brand brand = Brand.builder()
                .code("TEST_BRAND_010")
                .name("品牌10")
                .enable(true)
                .sort(10)
                .source(SourceType.MDM)
                .externalRefId(externalRefId)
                .externalVersion(1L)
                .lastSyncTime(LocalDateTime.now())
                .build();
        vehBrandRepository.insert(brand);

        // When
        Brand result = vehBrandRepository.selectByExternalRefId(externalRefId);

        // Then
        assertNotNull(result);
        assertEquals(externalRefId, result.getExternalRefId());
        assertEquals(SourceType.MDM, result.getSource());
    }

    @Test
    @DisplayName("应成功统计指定来源的品牌数量")
    void countBySource_shouldReturnCountForSource() {
        // Given
        Brand brandMdm = Brand.builder()
                .code("TEST_BRAND_011")
                .name("MDM品牌")
                .enable(true)
                .sort(11)
                .source(SourceType.MDM)
                .externalRefId("ext-011")
                .externalVersion(1L)
                .build();
        Brand brandManual = Brand.builder()
                .code("TEST_BRAND_012")
                .name("手动品牌")
                .enable(true)
                .sort(12)
                .source(SourceType.MANUAL)
                .build();
        vehBrandRepository.insert(brandMdm);
        vehBrandRepository.insert(brandManual);

        // When
        long mdmCount = vehBrandRepository.countBySource(SourceType.MDM);
        long manualCount = vehBrandRepository.countBySource(SourceType.MANUAL);

        // Then
        assertTrue(mdmCount >= 1);
        assertTrue(manualCount >= 1);
    }

    @Test
    @DisplayName("应成功更新MDM投影字段")
    void update_shouldSuccessfullyUpdateMdmProjectionFields() {
        // Given
        Brand brand = Brand.builder()
                .code("TEST_BRAND_013")
                .name("品牌13")
                .enable(true)
                .sort(13)
                .source(SourceType.MANUAL)
                .build();
        vehBrandRepository.insert(brand);

        // When
        brand.setSource(SourceType.MDM);
        brand.setExternalRefId("ext-013");
        brand.setExternalVersion(2L);
        brand.setLastSyncTime(LocalDateTime.now());
        int result = vehBrandRepository.update(brand);

        // Then
        assertEquals(1, result);
        Brand updatedBrand = vehBrandRepository.selectById(brand.getId());
        assertEquals(SourceType.MDM, updatedBrand.getSource());
        assertEquals("ext-013", updatedBrand.getExternalRefId());
        assertEquals(2L, updatedBrand.getExternalVersion());
        assertNotNull(updatedBrand.getLastSyncTime());
    }

    @Test
    @DisplayName("应成功插入MDM来源品牌并设置投影字段")
    void insert_shouldSuccessfullyInsertMdmBrandWithProjectionFields() {
        // Given
        Brand brand = Brand.builder()
                .code("TEST_BRAND_014")
                .name("MDM品牌14")
                .enable(true)
                .sort(14)
                .source(SourceType.MDM)
                .externalRefId("ext-014")
                .externalVersion(1L)
                .lastSyncTime(LocalDateTime.now())
                .build();

        // When
        int result = vehBrandRepository.insert(brand);

        // Then
        assertEquals(1, result);
        assertNotNull(brand.getId());

        Brand insertedBrand = vehBrandRepository.selectById(brand.getId());
        assertEquals(SourceType.MDM, insertedBrand.getSource());
        assertEquals("ext-014", insertedBrand.getExternalRefId());
        assertEquals(1L, insertedBrand.getExternalVersion());
        assertNotNull(insertedBrand.getLastSyncTime());
    }
}
