package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Configuration;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigurationOptionCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.ConfigurationHierarchy;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmConfigurationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MdmConfigurationRepository集成测试（CR-047：Configuration 最小投影收敛后）
 *
 * @author hwyz_leo
 */
@Rollback
class MdmConfigurationRepositoryImplTest extends BaseTest {

    @Autowired
    private MdmConfigurationRepository mdmConfigurationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM tb_mdm_configuration_option_code WHERE configuration_code LIKE 'TEST_CONFIG_%'");
        jdbcTemplate.execute("DELETE FROM tb_mdm_configuration WHERE code LIKE 'TEST_CONFIG_%'");
    }

    private Configuration buildConfiguration(String code, String variantCode) {
        return Configuration.builder()
                .code(code)
                .name("测试配置")
                .nameLocal("测试配置本地化")
                .variantCode(variantCode)
                .source(SourceType.MANUAL)
                .build();
    }

    @Test
    @DisplayName("应成功插入配置记录（最小投影字段）")
    void insert_shouldSuccessfullyInsertConfiguration() {
        // Given
        Configuration configuration = buildConfiguration("TEST_CONFIG_001", "V001");

        // When
        int result = mdmConfigurationRepository.insert(configuration);

        // Then
        assertEquals(1, result);
        assertNotNull(configuration.getId());
    }

    @Test
    @DisplayName("应成功根据ID查询配置")
    void selectById_shouldReturnConfigurationWhenExists() {
        // Given
        Configuration configuration = buildConfiguration("TEST_CONFIG_002", "V002");
        mdmConfigurationRepository.insert(configuration);

        // When
        Configuration result = mdmConfigurationRepository.selectById(configuration.getId());

        // Then
        assertNotNull(result);
        assertEquals(configuration.getId(), result.getId());
        assertEquals("TEST_CONFIG_002", result.getCode());
        assertEquals("测试配置", result.getName());
        assertEquals("测试配置本地化", result.getNameLocal());
        assertEquals("V002", result.getVariantCode());
    }

    @Test
    @DisplayName("应成功根据代码查询配置")
    void selectByCode_shouldReturnConfigurationWhenCodeExists() {
        // Given
        String code = "TEST_CONFIG_003";
        Configuration configuration = buildConfiguration(code, "V003");
        mdmConfigurationRepository.insert(configuration);

        // When
        Configuration result = mdmConfigurationRepository.selectByCode(code);

        // Then
        assertNotNull(result);
        assertEquals(code, result.getCode());
    }

    @Test
    @DisplayName("应成功更新配置信息")
    void update_shouldSuccessfullyUpdateConfiguration() {
        // Given
        Configuration configuration = buildConfiguration("TEST_CONFIG_004", "V004");
        mdmConfigurationRepository.insert(configuration);

        // When
        configuration.setName("更新后的名称");
        configuration.setNameLocal("更新后的本地化名称");
        int result = mdmConfigurationRepository.update(configuration);

        // Then
        assertEquals(1, result);
        Configuration updatedConfiguration = mdmConfigurationRepository.selectById(configuration.getId());
        assertEquals("更新后的名称", updatedConfiguration.getName());
        assertEquals("更新后的本地化名称", updatedConfiguration.getNameLocal());
    }

    @Test
    @DisplayName("应成功批量删除配置")
    void batchPhysicalDelete_shouldSuccessfullyDeleteConfigurations() {
        // Given
        Configuration configuration1 = buildConfiguration("TEST_CONFIG_005", "V005");
        Configuration configuration2 = buildConfiguration("TEST_CONFIG_006", "V006");
        mdmConfigurationRepository.insert(configuration1);
        mdmConfigurationRepository.insert(configuration2);

        Long[] ids = {configuration1.getId(), configuration2.getId()};

        // When
        int result = mdmConfigurationRepository.batchPhysicalDelete(ids);

        // Then
        assertEquals(2, result);
        assertNull(mdmConfigurationRepository.selectById(configuration1.getId()));
        assertNull(mdmConfigurationRepository.selectById(configuration2.getId()));
    }

    @Test
    @DisplayName("应成功根据条件查询配置列表")
    void selectByMap_shouldReturnConfigurationsMatchingCriteria() {
        // Given
        Configuration configuration1 = buildConfiguration("TEST_CONFIG_007", "V007");
        Configuration configuration2 = buildConfiguration("TEST_CONFIG_008", "V008");
        mdmConfigurationRepository.insert(configuration1);
        mdmConfigurationRepository.insert(configuration2);

        Map<String, Object> map = new HashMap<>();
        map.put("name", "%测试%");

        // When
        List<Configuration> result = mdmConfigurationRepository.selectByMap(map);

        // Then
        assertNotNull(result);
        assertTrue(result.stream().anyMatch(c -> "TEST_CONFIG_007".equals(c.getCode())));
    }

    @Test
    @DisplayName("应成功统计配置数量")
    void countByMap_shouldReturnCorrectCount() {
        // Given
        Configuration configuration = buildConfiguration("TEST_CONFIG_009", "V009");
        mdmConfigurationRepository.insert(configuration);

        Map<String, Object> map = new HashMap<>();
        map.put("code", "TEST_CONFIG_009");

        // When
        int result = mdmConfigurationRepository.countByMap(map);

        // Then
        assertEquals(1, result);
    }

    @Test
    @DisplayName("应成功根据外部引用ID查询配置")
    void selectByExternalRefId_shouldReturnConfigurationWhenExternalRefIdExists() {
        // Given
        String externalRefId = "ext-ref-001";
        Configuration configuration = Configuration.builder()
                .code("TEST_CONFIG_010")
                .name("配置10")
                .variantCode("V010")
                .source(SourceType.MDM)
                .externalRefId(externalRefId)
                .externalVersion(1L)
                .lastSyncTime(LocalDateTime.now())
                .build();
        mdmConfigurationRepository.insert(configuration);

        // When
        Configuration result = mdmConfigurationRepository.selectByExternalRefId(externalRefId);

        // Then
        assertNotNull(result);
        assertEquals(externalRefId, result.getExternalRefId());
        assertEquals(SourceType.MDM, result.getSource());
    }

    @Test
    @DisplayName("应成功统计指定来源的配置数量")
    void countBySource_shouldReturnCountForSource() {
        // Given
        Configuration configurationMdm = Configuration.builder()
                .code("TEST_CONFIG_011")
                .name("MDM配置")
                .variantCode("V011")
                .source(SourceType.MDM)
                .externalRefId("ext-011")
                .externalVersion(1L)
                .build();
        Configuration configurationManual = buildConfiguration("TEST_CONFIG_012", "V012");
        mdmConfigurationRepository.insert(configurationMdm);
        mdmConfigurationRepository.insert(configurationManual);

        // When
        long mdmCount = mdmConfigurationRepository.countBySource(SourceType.MDM);
        long manualCount = mdmConfigurationRepository.countBySource(SourceType.MANUAL);

        // Then
        assertTrue(mdmCount >= 1);
        assertTrue(manualCount >= 1);
    }

    @Test
    @DisplayName("应成功根据示例查询配置列表（按 variantCode）")
    void selectByExample_shouldReturnConfigurationsMatchingExample() {
        // Given
        Configuration configuration1 = buildConfiguration("TEST_CONFIG_013", "V013");
        Configuration configuration2 = buildConfiguration("TEST_CONFIG_014", "V014");
        mdmConfigurationRepository.insert(configuration1);
        mdmConfigurationRepository.insert(configuration2);

        Configuration example = Configuration.builder()
                .variantCode("V013")
                .build();

        // When
        List<Configuration> result = mdmConfigurationRepository.selectByExample(example);

        // Then
        assertNotNull(result);
        assertTrue(result.stream().anyMatch(c -> "TEST_CONFIG_013".equals(c.getCode())));
    }

    @Test
    @DisplayName("应成功插入MDM来源配置并设置投影字段")
    void insert_shouldSuccessfullyInsertMdmConfigurationWithProjectionFields() {
        // Given
        Configuration configuration = Configuration.builder()
                .code("TEST_CONFIG_015")
                .name("MDM配置15")
                .nameLocal("MDM配置15本地化")
                .variantCode("V015")
                .source(SourceType.MDM)
                .externalRefId("ext-015")
                .externalVersion(1L)
                .lastSyncTime(LocalDateTime.now())
                .build();

        // When
        int result = mdmConfigurationRepository.insert(configuration);

        // Then
        assertEquals(1, result);
        assertNotNull(configuration.getId());

        Configuration insertedConfiguration = mdmConfigurationRepository.selectById(configuration.getId());
        assertEquals(SourceType.MDM, insertedConfiguration.getSource());
        assertEquals("ext-015", insertedConfiguration.getExternalRefId());
        assertEquals(1L, insertedConfiguration.getExternalVersion());
        assertEquals("MDM配置15本地化", insertedConfiguration.getNameLocal());
        assertNotNull(insertedConfiguration.getLastSyncTime());
    }

    @Test
    @DisplayName("应成功更新MDM投影字段")
    void update_shouldSuccessfullyUpdateMdmProjectionFields() {
        // Given
        Configuration configuration = buildConfiguration("TEST_CONFIG_016", "V016");
        mdmConfigurationRepository.insert(configuration);

        // When
        configuration.setSource(SourceType.MDM);
        configuration.setExternalRefId("ext-016");
        configuration.setExternalVersion(2L);
        configuration.setLastSyncTime(LocalDateTime.now());
        int result = mdmConfigurationRepository.update(configuration);

        // Then
        assertEquals(1, result);
        Configuration updatedConfiguration = mdmConfigurationRepository.selectById(configuration.getId());
        assertEquals(SourceType.MDM, updatedConfiguration.getSource());
        assertEquals("ext-016", updatedConfiguration.getExternalRefId());
        assertEquals(2L, updatedConfiguration.getExternalVersion());
        assertNotNull(updatedConfiguration.getLastSyncTime());
    }

    @Test
    @DisplayName("应成功插入选项值并查询")
    void insertOptionCode_shouldSuccessfullyInsertAndQueryOptionCode() {
        // Given
        Configuration configuration = buildConfiguration("TEST_CONFIG_017", "V017");
        mdmConfigurationRepository.insert(configuration);

        ConfigurationOptionCode optionCode = ConfigurationOptionCode.builder()
                .configurationCode("TEST_CONFIG_017")
                .optionFamilyCode("FAMILY_001")
                .optionCode("OPTION_001")
                .optionType("EXTERIOR")
                .build();

        // When
        int insertResult = mdmConfigurationRepository.batchInsertOptionCode(Arrays.asList(optionCode));

        // Then
        assertEquals(1, insertResult);

        ConfigurationOptionCode queryExample = ConfigurationOptionCode.builder()
                .configurationCode("TEST_CONFIG_017")
                .optionFamilyCode("FAMILY_001")
                .build();
        List<ConfigurationOptionCode> optionCodes = mdmConfigurationRepository.selectOptionCodeByExample(queryExample);
        assertNotNull(optionCodes);
        assertFalse(optionCodes.isEmpty());
        assertEquals("OPTION_001", optionCodes.get(0).getOptionCode());
    }

    @Test
    @DisplayName("应成功更新选项值")
    void updateOptionCode_shouldSuccessfullyUpdateOptionCode() {
        // Given
        ConfigurationOptionCode optionCode = ConfigurationOptionCode.builder()
                .configurationCode("TEST_CONFIG_018")
                .optionFamilyCode("FAMILY_002")
                .optionCode("OPTION_002")
                .optionType("INTERIOR")
                .build();
        mdmConfigurationRepository.batchInsertOptionCode(Arrays.asList(optionCode));

        // When
        optionCode.setOptionType("UPDATED_TYPE");
        int result = mdmConfigurationRepository.updateOptionCode(optionCode);

        // Then
        assertEquals(1, result);
        ConfigurationOptionCode queryExample = ConfigurationOptionCode.builder()
                .configurationCode("TEST_CONFIG_018")
                .optionFamilyCode("FAMILY_002")
                .build();
        List<ConfigurationOptionCode> optionCodes = mdmConfigurationRepository.selectOptionCodeByExample(queryExample);
        assertNotNull(optionCodes);
        assertFalse(optionCodes.isEmpty());
        assertEquals("UPDATED_TYPE", optionCodes.get(0).getOptionType());
    }

    @Test
    @DisplayName("应成功批量删除选项值")
    void batchPhysicalDeleteOptionCode_shouldSuccessfullyDeleteOptionCodes() {
        // Given
        ConfigurationOptionCode optionCode1 = ConfigurationOptionCode.builder()
                .configurationCode("TEST_CONFIG_019")
                .optionFamilyCode("FAMILY_003")
                .optionCode("OPTION_003")
                .optionType("TYPE_A")
                .build();
        ConfigurationOptionCode optionCode2 = ConfigurationOptionCode.builder()
                .configurationCode("TEST_CONFIG_019")
                .optionFamilyCode("FAMILY_004")
                .optionCode("OPTION_004")
                .optionType("TYPE_B")
                .build();
        mdmConfigurationRepository.batchInsertOptionCode(Arrays.asList(optionCode1, optionCode2));

        // When
        Long[] ids = {optionCode1.getId(), optionCode2.getId()};
        int result = mdmConfigurationRepository.batchPhysicalDeleteOptionCode(ids);

        // Then
        assertEquals(2, result);
        ConfigurationOptionCode queryExample = ConfigurationOptionCode.builder()
                .configurationCode("TEST_CONFIG_019")
                .build();
        List<ConfigurationOptionCode> optionCodes = mdmConfigurationRepository.selectOptionCodeByExample(queryExample);
        assertTrue(optionCodes.isEmpty());
    }

    @Test
    @DisplayName("应成功按配置代码查询产品树补全视图（上层投影缺失时降级返回）")
    void selectHierarchyByCode_shouldReturnConfigurationWithDerivedHierarchy() {
        // Given
        jdbcTemplate.execute("DELETE FROM tb_mdm_brand WHERE code LIKE 'TEST_BRAND_%'");
        jdbcTemplate.execute("DELETE FROM tb_mdm_car_line WHERE code LIKE 'TEST_CARLINE_%'");
        jdbcTemplate.execute("DELETE FROM tb_mdm_platform WHERE code LIKE 'TEST_PLATFORM_%'");
        jdbcTemplate.execute("DELETE FROM tb_mdm_model WHERE code LIKE 'TEST_MODEL_%'");
        jdbcTemplate.execute("DELETE FROM tb_mdm_variant WHERE code LIKE 'TEST_VARIANT_%'");
        jdbcTemplate.execute("INSERT INTO tb_mdm_brand (code, name, enable, sort, source, row_valid) VALUES ('TEST_BRAND_047', '测试品牌', 1, 1, 'MANUAL', 1)");
        jdbcTemplate.execute("INSERT INTO tb_mdm_platform (code, name, enable, sort, source, row_valid) VALUES ('TEST_PLATFORM_047', '测试平台', 1, 1, 'MANUAL', 1)");
        jdbcTemplate.execute("INSERT INTO tb_mdm_car_line (code, name, brand_code, enable, sort, source, row_valid) VALUES ('TEST_CARLINE_047', '测试车系', 'TEST_BRAND_047', 1, 1, 'MANUAL', 1)");
        jdbcTemplate.execute("INSERT INTO tb_mdm_model (code, name, platform_code, car_line_code, enable, sort, source, row_valid) VALUES ('TEST_MODEL_047', '测试车型', 'TEST_PLATFORM_047', 'TEST_CARLINE_047', 1, 1, 'MANUAL', 1)");
        jdbcTemplate.execute("INSERT INTO tb_mdm_variant (code, name, platform_code, car_line_code, model_code, enable, sort, source, row_valid) VALUES ('TEST_VARIANT_047', '测试版本', 'TEST_PLATFORM_047', 'TEST_CARLINE_047', 'TEST_MODEL_047', 1, 1, 'MANUAL', 1)");

        Configuration configuration = buildConfiguration("TEST_CONFIG_047", "TEST_VARIANT_047");
        mdmConfigurationRepository.insert(configuration);

        // When
        ConfigurationHierarchy hierarchy = mdmConfigurationRepository.selectHierarchyByCode("TEST_CONFIG_047");

        // Then
        assertNotNull(hierarchy);
        assertEquals("TEST_CONFIG_047", hierarchy.getCode());
        assertEquals("TEST_VARIANT_047", hierarchy.getVariantCode());
        assertEquals("TEST_MODEL_047", hierarchy.getModelCode());
        assertEquals("TEST_CARLINE_047", hierarchy.getCarLineCode());
        assertEquals("TEST_PLATFORM_047", hierarchy.getPlatformCode());
        assertEquals("TEST_BRAND_047", hierarchy.getBrandCode());
    }

    @Test
    @DisplayName("应成功在产品树上层投影缺失时降级返回（LEFT JOIN，派生字段为null）")
    void selectHierarchyByCode_shouldReturnBasicInfoWhenUpperProjectionMissing() {
        // Given
        Configuration configuration = buildConfiguration("TEST_CONFIG_048", "TEST_VARIANT_MISSING_048");
        mdmConfigurationRepository.insert(configuration);

        // When
        ConfigurationHierarchy hierarchy = mdmConfigurationRepository.selectHierarchyByCode("TEST_CONFIG_048");

        // Then
        assertNotNull(hierarchy);
        assertEquals("TEST_CONFIG_048", hierarchy.getCode());
        assertEquals("测试配置", hierarchy.getName());
        assertNull(hierarchy.getModelCode());
        assertNull(hierarchy.getCarLineCode());
        assertNull(hierarchy.getPlatformCode());
        assertNull(hierarchy.getBrandCode());
    }

    @Test
    @DisplayName("应成功按配置代码批量查询产品树补全视图（IN + JOIN）")
    void selectHierarchyByCodes_shouldReturnBatchHierarchy() {
        // Given
        jdbcTemplate.execute("DELETE FROM tb_mdm_variant WHERE code LIKE 'TEST_VARIANT_%'");
        jdbcTemplate.execute("INSERT INTO tb_mdm_variant (code, name, platform_code, car_line_code, model_code, enable, sort, source, row_valid) VALUES ('TEST_VARIANT_A', '版本A', 'P_A', 'CL_A', 'M_A', 1, 1, 'MANUAL', 1)");
        Configuration configuration1 = buildConfiguration("TEST_CONFIG_049", "TEST_VARIANT_A");
        Configuration configuration2 = buildConfiguration("TEST_CONFIG_050", "TEST_VARIANT_A");
        mdmConfigurationRepository.insert(configuration1);
        mdmConfigurationRepository.insert(configuration2);

        // When
        List<ConfigurationHierarchy> hierarchies = mdmConfigurationRepository.selectHierarchyByCodes(Arrays.asList("TEST_CONFIG_049", "TEST_CONFIG_050"));

        // Then
        assertNotNull(hierarchies);
        assertEquals(2, hierarchies.size());
        assertTrue(hierarchies.stream().anyMatch(h -> "TEST_CONFIG_049".equals(h.getCode())));
        assertTrue(hierarchies.stream().anyMatch(h -> "TEST_CONFIG_050".equals(h.getCode())));
    }

    @Test
    @DisplayName("应成功按产品树条件筛选配置（modelCode 过滤）")
    void selectHierarchyByMap_shouldFilterByModelCode() {
        // Given
        jdbcTemplate.execute("DELETE FROM tb_mdm_model WHERE code LIKE 'TEST_MODEL_%'");
        jdbcTemplate.execute("DELETE FROM tb_mdm_variant WHERE code LIKE 'TEST_VARIANT_%'");
        jdbcTemplate.execute("INSERT INTO tb_mdm_model (code, name, platform_code, car_line_code, enable, sort, source, row_valid) VALUES ('TEST_MODEL_FILTER', '车型筛选', 'P_F', 'CL_F', 1, 1, 'MANUAL', 1)");
        jdbcTemplate.execute("INSERT INTO tb_mdm_variant (code, name, platform_code, car_line_code, model_code, enable, sort, source, row_valid) VALUES ('TEST_VARIANT_FILTER', '版本筛选', 'P_F', 'CL_F', 'TEST_MODEL_FILTER', 1, 1, 'MANUAL', 1)");
        Configuration configuration1 = buildConfiguration("TEST_CONFIG_051", "TEST_VARIANT_FILTER");
        Configuration configuration2 = buildConfiguration("TEST_CONFIG_052", "TEST_VARIANT_OTHER");
        mdmConfigurationRepository.insert(configuration1);
        mdmConfigurationRepository.insert(configuration2);

        Map<String, Object> map = new HashMap<>();
        map.put("modelCode", "TEST_MODEL_FILTER");

        // When
        List<ConfigurationHierarchy> result = mdmConfigurationRepository.selectHierarchyByMap(map);

        // Then
        assertNotNull(result);
        assertTrue(result.stream().anyMatch(c -> "TEST_CONFIG_051".equals(c.getCode())));
        assertTrue(result.stream().noneMatch(c -> "TEST_CONFIG_052".equals(c.getCode())));
    }

    @Test
    @DisplayName("应成功逻辑删除配置（CR-047 DELETED/DEACTIVATED 事件语义）")
    void logicalDeleteById_shouldMarkRowInvalid() {
        // Given
        Configuration configuration = buildConfiguration("TEST_CONFIG_053", "V053");
        mdmConfigurationRepository.insert(configuration);

        // When
        int result = mdmConfigurationRepository.logicalDeleteById(configuration.getId());

        // Then
        assertEquals(1, result);
        assertNull(mdmConfigurationRepository.selectById(configuration.getId()));
    }

    @Test
    @DisplayName("应成功统计缺失Variant引用的配置条数")
    void countMissingVariant_shouldReturnCount() {
        // Given
        Configuration configuration = buildConfiguration("TEST_CONFIG_054", "TEST_VARIANT_NOT_EXIST_054");
        mdmConfigurationRepository.insert(configuration);

        // When
        long missingVariant = mdmConfigurationRepository.countMissingVariant();

        // Then
        assertTrue(missingVariant >= 1);
    }
}
