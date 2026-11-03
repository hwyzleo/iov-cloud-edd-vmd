package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Brand;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmBrandRepository;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.mapper.MdmBrandMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MdmBrandRepository集成测试
 *
 * @author hwyz_leo
 */
@Rollback
class MdmBrandRepositoryTest extends BaseTest {

    @Autowired
    private MdmBrandRepository mdmBrandRepository;

    @Autowired
    private MdmBrandMapper mdmBrandMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM tb_mdm_brand WHERE code LIKE 'TEST_BRAND_%'");
    }

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
        int result = mdmBrandRepository.insert(brand);

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
        mdmBrandRepository.insert(brand);

        // When
        Brand result = mdmBrandRepository.selectById(brand.getId());

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
        mdmBrandRepository.insert(brand);

        // When
        Brand result = mdmBrandRepository.selectByCode(code);

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
        mdmBrandRepository.insert(brand);

        // When
        brand.setName("更新后的名称");
        brand.setSort(10);
        int result = mdmBrandRepository.update(brand);

        // Then
        assertEquals(1, result);
        Brand updatedBrand = mdmBrandRepository.selectById(brand.getId());
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
        mdmBrandRepository.insert(brand1);
        mdmBrandRepository.insert(brand2);

        Long[] ids = {brand1.getId(), brand2.getId()};

        // When
        int result = mdmBrandRepository.batchPhysicalDelete(ids);

        // Then
        assertEquals(2, result);
        assertNull(mdmBrandRepository.selectById(brand1.getId()));
        assertNull(mdmBrandRepository.selectById(brand2.getId()));
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
        mdmBrandRepository.insert(brand1);
        mdmBrandRepository.insert(brand2);

        Map<String, Object> map = new HashMap<>();
        map.put("name", "%测试%");

        // When
        List<Brand> result = mdmBrandRepository.selectByMap(map);

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
        mdmBrandRepository.insert(brand);

        Map<String, Object> map = new HashMap<>();
        map.put("code", "TEST_BRAND_009");

        // When
        int result = mdmBrandRepository.countByMap(map);

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
        mdmBrandRepository.insert(brand);

        // When
        Brand result = mdmBrandRepository.selectByExternalRefId(externalRefId);

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
        mdmBrandRepository.insert(brandMdm);
        mdmBrandRepository.insert(brandManual);

        // When
        long mdmCount = mdmBrandRepository.countBySource(SourceType.MDM);
        long manualCount = mdmBrandRepository.countBySource(SourceType.MANUAL);

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
        mdmBrandRepository.insert(brand);

        // When
        brand.setSource(SourceType.MDM);
        brand.setExternalRefId("ext-013");
        brand.setExternalVersion(2L);
        brand.setLastSyncTime(LocalDateTime.now());
        int result = mdmBrandRepository.update(brand);

        // Then
        assertEquals(1, result);
        Brand updatedBrand = mdmBrandRepository.selectById(brand.getId());
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
        int result = mdmBrandRepository.insert(brand);

        // Then
        assertEquals(1, result);
        assertNotNull(brand.getId());

        Brand insertedBrand = mdmBrandRepository.selectById(brand.getId());
        assertEquals(SourceType.MDM, insertedBrand.getSource());
        assertEquals("ext-014", insertedBrand.getExternalRefId());
        assertEquals(1L, insertedBrand.getExternalVersion());
        assertNotNull(insertedBrand.getLastSyncTime());
    }

    @Test
    @DisplayName("插入后应回填行版本并保持 DB 生成时间与 row_valid=1（CR-075 fill/时间）")
    void insert_shouldBackfillRowVersionAndDbGeneratedTimes() {
        // Given
        Brand brand = Brand.builder()
                .code("TEST_BRAND_015")
                .name("CR075品牌")
                .enable(true)
                .sort(15)
                .source(SourceType.MANUAL)
                .build();

        // When
        mdmBrandRepository.insert(brand);

        // Then
        assertEquals(1, brand.getRowVersion(), "insert 后应回填 rowVersion=1");
        Brand inserted = mdmBrandRepository.selectById(brand.getId());
        assertNotNull(inserted);
        assertNotNull(jdbcTemplate.queryForObject(
                "SELECT create_time FROM tb_mdm_brand WHERE id = ?", java.util.Date.class, brand.getId()),
                "create_time 应由 DB now() 生成");
        assertNotNull(jdbcTemplate.queryForObject(
                "SELECT modify_time FROM tb_mdm_brand WHERE id = ?", java.util.Date.class, brand.getId()),
                "modify_time 应由 DB now() 生成");
        Integer rowVersion = jdbcTemplate.queryForObject(
                "SELECT row_version FROM tb_mdm_brand WHERE id = ?", Integer.class, brand.getId());
        assertEquals(1, rowVersion);
        Boolean rowValid = jdbcTemplate.queryForObject(
                "SELECT row_valid FROM tb_mdm_brand WHERE id = ?", Boolean.class, brand.getId());
        assertEquals(Boolean.TRUE, rowValid);
    }

    @Test
    @DisplayName("使用过期版本更新应返回 0（CAS 冲突）且不改内存版本（CR-075）")
    void update_shouldReturnZeroOnStaleVersionCasConflict() {
        // Given
        Brand brand = Brand.builder()
                .code("TEST_BRAND_016")
                .name("CAS品牌")
                .enable(true)
                .sort(16)
                .source(SourceType.MANUAL)
                .build();
        mdmBrandRepository.insert(brand);

        // When: 携带过期版本（模拟并发后陈旧读取）
        brand.setName("冲突更新");
        brand.setRowVersion(999);
        int result = mdmBrandRepository.update(brand);

        // Then
        assertEquals(0, result, "过期版本 CAS 应返回 0");
        assertEquals(999, brand.getRowVersion(), "失败后不得修改内存版本（不复用）");
        Brand inDb = mdmBrandRepository.selectById(brand.getId());
        assertEquals("CAS品牌", inDb.getName(), "数据不得被陈旧更新覆盖");
        Integer rowVersion = jdbcTemplate.queryForObject(
                "SELECT row_version FROM tb_mdm_brand WHERE id = ?", Integer.class, brand.getId());
        assertEquals(1, rowVersion, "冲突失败后版本不得递增");
    }

    @Test
    @DisplayName("CAS 更新成功后应回传新版本（旧版本+1）（CR-075）")
    void update_shouldRoundTripVersionAfterSuccess() {
        // Given
        Brand brand = Brand.builder()
                .code("TEST_BRAND_017")
                .name("版本回传")
                .enable(true)
                .sort(17)
                .source(SourceType.MANUAL)
                .build();
        mdmBrandRepository.insert(brand);
        assertEquals(1, brand.getRowVersion());

        // When
        brand.setName("更新后");
        int result = mdmBrandRepository.update(brand);

        // Then
        assertEquals(1, result);
        assertEquals(2, brand.getRowVersion(), "成功后应回传新版本");
        Integer rowVersion = jdbcTemplate.queryForObject(
                "SELECT row_version FROM tb_mdm_brand WHERE id = ?", Integer.class, brand.getId());
        assertEquals(2, rowVersion);
    }

    @Test
    @DisplayName("逻辑删除应保留版本递增与修改时间等审计副作用（CR-075 专用 SQL）")
    void logicalDelete_shouldKeepAuditSideEffects() {
        // Given
        Brand brand = Brand.builder()
                .code("TEST_BRAND_018")
                .name("逻辑删除")
                .enable(true)
                .sort(18)
                .source(SourceType.MANUAL)
                .build();
        mdmBrandRepository.insert(brand);

        // When: 直接调用保留的专用逻辑删除 SQL
        int result = mdmBrandMapper.logicalDeletePo(brand.getId());

        // Then
        assertEquals(1, result);
        assertNull(mdmBrandRepository.selectById(brand.getId()), "MP selectById 应过滤逻辑删除行");
        Boolean rowValid = jdbcTemplate.queryForObject(
                "SELECT row_valid FROM tb_mdm_brand WHERE id = ?", Boolean.class, brand.getId());
        assertEquals(Boolean.FALSE, rowValid, "row_valid 应置 0");
        Integer rowVersion = jdbcTemplate.queryForObject(
                "SELECT row_version FROM tb_mdm_brand WHERE id = ?", Integer.class, brand.getId());
        assertEquals(2, rowVersion, "逻辑删除应递增版本");
        assertNotNull(jdbcTemplate.queryForObject(
                "SELECT modify_time FROM tb_mdm_brand WHERE id = ?", java.util.Date.class, brand.getId()),
                "modify_time 应更新");
    }

    @Test
    @DisplayName("空版本更新应返回 0（CAS 拒绝，禁止自动补版本，CR-075）")
    void update_shouldReturnZeroWhenVersionNull() {
        // Given
        Brand brand = Brand.builder()
                .code("TEST_BRAND_019")
                .name("空版本")
                .enable(true)
                .sort(19)
                .source(SourceType.MANUAL)
                .build();
        mdmBrandRepository.insert(brand);
        Brand noVersion = mdmBrandRepository.selectById(brand.getId());
        noVersion.setRowVersion(null); // 模拟未携带可信版本
        noVersion.setName("不应成功");

        // When
        int result = mdmBrandRepository.update(noVersion);

        // Then
        assertEquals(0, result, "空版本 CAS 必须拒绝");
        Brand inDb = mdmBrandRepository.selectById(brand.getId());
        assertEquals("空版本", inDb.getName());
    }

    @Test
    @DisplayName("同版本并发更新仅一条成功（CAS WHERE 原子性，CR-075）")
    void update_shouldAllowOnlyOneCasWinner() {
        // Given
        Brand brand = Brand.builder()
                .code("TEST_BRAND_020")
                .name("并发CAS")
                .enable(true)
                .sort(20)
                .source(SourceType.MANUAL)
                .build();
        mdmBrandRepository.insert(brand);
        assertEquals(1, brand.getRowVersion());

        // When：模拟两条携带相同原版本的并发更新（同 WHERE row_version=1）
        int first = mdmBrandRepository.update(brand);
        int second = mdmBrandRepository.update(brand); // 内存版本已回传为 2，成功
        brand.setRowVersion(1); // 强制回退模拟并发陈旧读取
        int staleSecond = mdmBrandRepository.update(brand);

        // Then：仅第一条成功；陈旧并发更新返回 0
        assertEquals(1, first);
        assertEquals(1, second);
        assertEquals(0, staleSecond, "同版本并发第二次更新必须失败");
        Integer rowVersion = jdbcTemplate.queryForObject(
                "SELECT row_version FROM tb_mdm_brand WHERE id = ?", Integer.class, brand.getId());
        assertEquals(3, rowVersion, "仅成功路径递增版本");
    }

    @Test
    @DisplayName("空数组批量物理删除应返回 0（受控硬删除边界，CR-075）")
    void batchPhysicalDelete_shouldReturnZeroForEmptyArray() {
        // When
        int result = mdmBrandRepository.batchPhysicalDelete(new Long[0]);

        // Then
        assertEquals(0, result);
    }

    @Test
    @DisplayName("不存在 ID 的批量物理删除应返回 0（非法 ID 无副作用，CR-075）")
    void batchPhysicalDelete_shouldReturnZeroForNonexistentIds() {
        // When
        int result = mdmBrandRepository.batchPhysicalDelete(new Long[]{999999L, 999998L});

        // Then
        assertEquals(0, result);
    }
}
