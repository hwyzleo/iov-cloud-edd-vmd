package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Platform;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmPlatformRepository;
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
 * MdmPlatformRepository集成测试
 *
 * @author hwyz_leo
 */
@Rollback
class MdmPlatformRepositoryTest extends BaseTest {

    @Autowired
    private MdmPlatformRepository mdmPlatformRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM tb_mdm_platform WHERE code LIKE 'TEST_PLATFORM_%'");
    }

    @Test
    @DisplayName("应成功插入平台记录")
    void insert_shouldSuccessfullyInsertPlatform() {
        // Given
        Platform platform = Platform.builder()
                .code("TEST_PLATFORM_001")
                .name("测试平台")
                .nameEn("Test Platform")
                .enable(true)
                .sort(1)
                .source(SourceType.MANUAL)
                .build();

        // When
        int result = mdmPlatformRepository.insert(platform);

        // Then
        assertEquals(1, result);
        assertNotNull(platform.getId());
    }

    @Test
    @DisplayName("应成功根据ID查询平台")
    void selectById_shouldReturnPlatformWhenExists() {
        // Given
        Platform platform = Platform.builder()
                .code("TEST_PLATFORM_002")
                .name("测试平台2")
                .enable(true)
                .sort(2)
                .source(SourceType.MANUAL)
                .build();
        mdmPlatformRepository.insert(platform);

        // When
        Platform result = mdmPlatformRepository.selectById(platform.getId());

        // Then
        assertNotNull(result);
        assertEquals(platform.getId(), result.getId());
        assertEquals("TEST_PLATFORM_002", result.getCode());
        assertEquals("测试平台2", result.getName());
    }

    @Test
    @DisplayName("应成功根据代码查询平台")
    void selectByCode_shouldReturnPlatformWhenCodeExists() {
        // Given
        String code = "TEST_PLATFORM_003";
        Platform platform = Platform.builder()
                .code(code)
                .name("测试平台3")
                .enable(true)
                .sort(3)
                .source(SourceType.MANUAL)
                .build();
        mdmPlatformRepository.insert(platform);

        // When
        Platform result = mdmPlatformRepository.selectByCode(code);

        // Then
        assertNotNull(result);
        assertEquals(code, result.getCode());
    }

    @Test
    @DisplayName("应成功更新平台信息")
    void update_shouldSuccessfullyUpdatePlatform() {
        // Given
        Platform platform = Platform.builder()
                .code("TEST_PLATFORM_004")
                .name("原始名称")
                .enable(true)
                .sort(4)
                .source(SourceType.MANUAL)
                .build();
        mdmPlatformRepository.insert(platform);

        // When
        platform.setName("更新后的名称");
        platform.setSort(10);
        int result = mdmPlatformRepository.update(platform);

        // Then
        assertEquals(1, result);
        Platform updatedPlatform = mdmPlatformRepository.selectById(platform.getId());
        assertEquals("更新后的名称", updatedPlatform.getName());
        assertEquals(10, updatedPlatform.getSort());
    }

    @Test
    @DisplayName("应成功批量删除平台")
    void batchPhysicalDelete_shouldSuccessfullyDeletePlatforms() {
        // Given
        Platform platform1 = Platform.builder()
                .code("TEST_PLATFORM_005")
                .name("平台5")
                .enable(true)
                .sort(5)
                .source(SourceType.MANUAL)
                .build();
        Platform platform2 = Platform.builder()
                .code("TEST_PLATFORM_006")
                .name("平台6")
                .enable(true)
                .sort(6)
                .source(SourceType.MANUAL)
                .build();
        mdmPlatformRepository.insert(platform1);
        mdmPlatformRepository.insert(platform2);

        Long[] ids = {platform1.getId(), platform2.getId()};

        // When
        int result = mdmPlatformRepository.batchPhysicalDelete(ids);

        // Then
        assertEquals(2, result);
        assertNull(mdmPlatformRepository.selectById(platform1.getId()));
        assertNull(mdmPlatformRepository.selectById(platform2.getId()));
    }

    @Test
    @DisplayName("应成功根据条件查询平台列表")
    void selectByMap_shouldReturnPlatformsMatchingCriteria() {
        // Given
        Platform platform1 = Platform.builder()
                .code("TEST_PLATFORM_007")
                .name("测试平台7")
                .enable(true)
                .sort(7)
                .source(SourceType.MANUAL)
                .build();
        Platform platform2 = Platform.builder()
                .code("TEST_PLATFORM_008")
                .name("其他平台")
                .enable(true)
                .sort(8)
                .source(SourceType.MANUAL)
                .build();
        mdmPlatformRepository.insert(platform1);
        mdmPlatformRepository.insert(platform2);

        Map<String, Object> map = new HashMap<>();
        map.put("name", "%测试%");

        // When
        List<Platform> result = mdmPlatformRepository.selectByMap(map);

        // Then
        assertNotNull(result);
        assertTrue(result.stream().anyMatch(p -> "TEST_PLATFORM_007".equals(p.getCode())));
    }

    @Test
    @DisplayName("应成功统计平台数量")
    void countByMap_shouldReturnCorrectCount() {
        // Given
        Platform platform = Platform.builder()
                .code("TEST_PLATFORM_009")
                .name("平台9")
                .enable(true)
                .sort(9)
                .source(SourceType.MANUAL)
                .build();
        mdmPlatformRepository.insert(platform);

        Map<String, Object> map = new HashMap<>();
        map.put("code", "TEST_PLATFORM_009");

        // When
        int result = mdmPlatformRepository.countByMap(map);

        // Then
        assertEquals(1, result);
    }

    @Test
    @DisplayName("应成功根据外部引用ID查询平台")
    void selectByExternalRefId_shouldReturnPlatformWhenExternalRefIdExists() {
        // Given
        String externalRefId = "ext-ref-001";
        Platform platform = Platform.builder()
                .code("TEST_PLATFORM_010")
                .name("平台10")
                .enable(true)
                .sort(10)
                .source(SourceType.MDM)
                .externalRefId(externalRefId)
                .externalVersion(1L)
                .lastSyncTime(LocalDateTime.now())
                .build();
        mdmPlatformRepository.insert(platform);

        // When
        Platform result = mdmPlatformRepository.selectByExternalRefId(externalRefId);

        // Then
        assertNotNull(result);
        assertEquals(externalRefId, result.getExternalRefId());
        assertEquals(SourceType.MDM, result.getSource());
    }

    @Test
    @DisplayName("应成功统计指定来源的平台数量")
    void countBySource_shouldReturnCountForSource() {
        // Given
        Platform platformMdm = Platform.builder()
                .code("TEST_PLATFORM_011")
                .name("MDM平台")
                .enable(true)
                .sort(11)
                .source(SourceType.MDM)
                .externalRefId("ext-011")
                .externalVersion(1L)
                .build();
        Platform platformManual = Platform.builder()
                .code("TEST_PLATFORM_012")
                .name("手动平台")
                .enable(true)
                .sort(12)
                .source(SourceType.MANUAL)
                .build();
        mdmPlatformRepository.insert(platformMdm);
        mdmPlatformRepository.insert(platformManual);

        // When
        long mdmCount = mdmPlatformRepository.countBySource(SourceType.MDM);
        long manualCount = mdmPlatformRepository.countBySource(SourceType.MANUAL);

        // Then
        assertTrue(mdmCount >= 1);
        assertTrue(manualCount >= 1);
    }

    @Test
    @DisplayName("应成功更新MDM投影字段")
    void update_shouldSuccessfullyUpdateMdmProjectionFields() {
        // Given
        Platform platform = Platform.builder()
                .code("TEST_PLATFORM_013")
                .name("平台13")
                .enable(true)
                .sort(13)
                .source(SourceType.MANUAL)
                .build();
        mdmPlatformRepository.insert(platform);

        // When
        platform.setSource(SourceType.MDM);
        platform.setExternalRefId("ext-013");
        platform.setExternalVersion(2L);
        platform.setLastSyncTime(LocalDateTime.now());
        int result = mdmPlatformRepository.update(platform);

        // Then
        assertEquals(1, result);
        Platform updatedPlatform = mdmPlatformRepository.selectById(platform.getId());
        assertEquals(SourceType.MDM, updatedPlatform.getSource());
        assertEquals("ext-013", updatedPlatform.getExternalRefId());
        assertEquals(2L, updatedPlatform.getExternalVersion());
        assertNotNull(updatedPlatform.getLastSyncTime());
    }

    @Test
    @DisplayName("应成功插入MDM来源平台并设置投影字段")
    void insert_shouldSuccessfullyInsertMdmPlatformWithProjectionFields() {
        // Given
        Platform platform = Platform.builder()
                .code("TEST_PLATFORM_014")
                .name("MDM平台14")
                .enable(true)
                .sort(14)
                .source(SourceType.MDM)
                .externalRefId("ext-014")
                .externalVersion(1L)
                .lastSyncTime(LocalDateTime.now())
                .build();

        // When
        int result = mdmPlatformRepository.insert(platform);

        // Then
        assertEquals(1, result);
        assertNotNull(platform.getId());

        Platform insertedPlatform = mdmPlatformRepository.selectById(platform.getId());
        assertEquals(SourceType.MDM, insertedPlatform.getSource());
        assertEquals("ext-014", insertedPlatform.getExternalRefId());
        assertEquals(1L, insertedPlatform.getExternalVersion());
        assertNotNull(insertedPlatform.getLastSyncTime());
    }
}
