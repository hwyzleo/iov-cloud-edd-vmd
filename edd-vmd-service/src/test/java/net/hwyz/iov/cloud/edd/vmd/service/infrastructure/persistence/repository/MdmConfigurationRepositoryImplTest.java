package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.repository;

import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Configuration;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.ConfigurationOptionCode;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmConfigurationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MdmConfigurationRepository集成测试
 *
 * @author hwyz_leo
 */
@Rollback
class MdmConfigurationRepositoryImplTest extends BaseTest {

    @Autowired
    private MdmConfigurationRepository mdmConfigurationRepository;

    @Test
    @DisplayName("应成功插入配置记录")
    void insert_shouldSuccessfullyInsertConfiguration() {
        // Given
        Configuration configuration = Configuration.builder()
                .code("TEST_CONFIG_001")
                .name("测试配置")
                .nameEn("Test Configuration")
                .platformCode("P001")
                .carLineCode("CL001")
                .modelCode("M001")
                .variantCode("V001")
                .vehicleStageCode("VS001")
                .enable(true)
                .sort(1)
                .source(SourceType.MANUAL)
                .build();

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
        Configuration configuration = Configuration.builder()
                .code("TEST_CONFIG_002")
                .name("测试配置2")
                .platformCode("P002")
                .carLineCode("CL002")
                .modelCode("M002")
                .variantCode("V002")
                .enable(true)
                .sort(2)
                .source(SourceType.MANUAL)
                .build();
        mdmConfigurationRepository.insert(configuration);

        // When
        Configuration result = mdmConfigurationRepository.selectById(configuration.getId());

        // Then
        assertNotNull(result);
        assertEquals(configuration.getId(), result.getId());
        assertEquals("TEST_CONFIG_002", result.getCode());
        assertEquals("测试配置2", result.getName());
    }

    @Test
    @DisplayName("应成功根据代码查询配置")
    void selectByCode_shouldReturnConfigurationWhenCodeExists() {
        // Given
        String code = "TEST_CONFIG_003";
        Configuration configuration = Configuration.builder()
                .code(code)
                .name("测试配置3")
                .platformCode("P003")
                .carLineCode("CL003")
                .modelCode("M003")
                .variantCode("V003")
                .enable(true)
                .sort(3)
                .source(SourceType.MANUAL)
                .build();
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
        Configuration configuration = Configuration.builder()
                .code("TEST_CONFIG_004")
                .name("原始名称")
                .platformCode("P004")
                .carLineCode("CL004")
                .modelCode("M004")
                .variantCode("V004")
                .enable(true)
                .sort(4)
                .source(SourceType.MANUAL)
                .build();
        mdmConfigurationRepository.insert(configuration);

        // When
        configuration.setName("更新后的名称");
        configuration.setSort(10);
        int result = mdmConfigurationRepository.update(configuration);

        // Then
        assertEquals(1, result);
        Configuration updatedConfiguration = mdmConfigurationRepository.selectById(configuration.getId());
        assertEquals("更新后的名称", updatedConfiguration.getName());
        assertEquals(10, updatedConfiguration.getSort());
    }

    @Test
    @DisplayName("应成功批量删除配置")
    void batchPhysicalDelete_shouldSuccessfullyDeleteConfigurations() {
        // Given
        Configuration configuration1 = Configuration.builder()
                .code("TEST_CONFIG_005")
                .name("配置5")
                .platformCode("P005")
                .carLineCode("CL005")
                .modelCode("M005")
                .variantCode("V005")
                .enable(true)
                .sort(5)
                .source(SourceType.MANUAL)
                .build();
        Configuration configuration2 = Configuration.builder()
                .code("TEST_CONFIG_006")
                .name("配置6")
                .platformCode("P006")
                .carLineCode("CL006")
                .modelCode("M006")
                .variantCode("V006")
                .enable(true)
                .sort(6)
                .source(SourceType.MANUAL)
                .build();
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
        Configuration configuration1 = Configuration.builder()
                .code("TEST_CONFIG_007")
                .name("测试配置7")
                .platformCode("P007")
                .carLineCode("CL007")
                .modelCode("M007")
                .variantCode("V007")
                .enable(true)
                .sort(7)
                .source(SourceType.MANUAL)
                .build();
        Configuration configuration2 = Configuration.builder()
                .code("TEST_CONFIG_008")
                .name("其他配置")
                .platformCode("P008")
                .carLineCode("CL008")
                .modelCode("M008")
                .variantCode("V008")
                .enable(true)
                .sort(8)
                .source(SourceType.MANUAL)
                .build();
        mdmConfigurationRepository.insert(configuration1);
        mdmConfigurationRepository.insert(configuration2);

        Map<String, Object> map = new HashMap<>();
        map.put("name", "测试");

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
        Configuration configuration = Configuration.builder()
                .code("TEST_CONFIG_009")
                .name("配置9")
                .platformCode("P009")
                .carLineCode("CL009")
                .modelCode("M009")
                .variantCode("V009")
                .enable(true)
                .sort(9)
                .source(SourceType.MANUAL)
                .build();
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
                .platformCode("P010")
                .carLineCode("CL010")
                .modelCode("M010")
                .variantCode("V010")
                .enable(true)
                .sort(10)
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
                .platformCode("P011")
                .carLineCode("CL011")
                .modelCode("M011")
                .variantCode("V011")
                .enable(true)
                .sort(11)
                .source(SourceType.MDM)
                .externalRefId("ext-011")
                .externalVersion(1L)
                .build();
        Configuration configurationManual = Configuration.builder()
                .code("TEST_CONFIG_012")
                .name("手动配置")
                .platformCode("P012")
                .carLineCode("CL012")
                .modelCode("M012")
                .variantCode("V012")
                .enable(true)
                .sort(12)
                .source(SourceType.MANUAL)
                .build();
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
    @DisplayName("应成功根据示例查询配置列表")
    void selectByExample_shouldReturnConfigurationsMatchingExample() {
        // Given
        Configuration configuration1 = Configuration.builder()
                .code("TEST_CONFIG_013")
                .name("示例配置13")
                .platformCode("P013")
                .carLineCode("CL013")
                .modelCode("M013")
                .variantCode("V013")
                .enable(true)
                .sort(13)
                .source(SourceType.MANUAL)
                .build();
        Configuration configuration2 = Configuration.builder()
                .code("TEST_CONFIG_014")
                .name("示例配置14")
                .platformCode("P014")
                .carLineCode("CL014")
                .modelCode("M014")
                .variantCode("V014")
                .enable(true)
                .sort(14)
                .source(SourceType.MANUAL)
                .build();
        mdmConfigurationRepository.insert(configuration1);
        mdmConfigurationRepository.insert(configuration2);

        Configuration example = Configuration.builder()
                .platformCode("P013")
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
                .platformCode("P015")
                .carLineCode("CL015")
                .modelCode("M015")
                .variantCode("V015")
                .enable(true)
                .sort(15)
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
        assertNotNull(insertedConfiguration.getLastSyncTime());
    }

    @Test
    @DisplayName("应成功更新MDM投影字段")
    void update_shouldSuccessfullyUpdateMdmProjectionFields() {
        // Given
        Configuration configuration = Configuration.builder()
                .code("TEST_CONFIG_016")
                .name("配置16")
                .platformCode("P016")
                .carLineCode("CL016")
                .modelCode("M016")
                .variantCode("V016")
                .enable(true)
                .sort(16)
                .source(SourceType.MANUAL)
                .build();
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
        Configuration configuration = Configuration.builder()
                .code("TEST_CONFIG_017")
                .name("配置17")
                .platformCode("P017")
                .carLineCode("CL017")
                .modelCode("M017")
                .variantCode("V017")
                .enable(true)
                .sort(17)
                .source(SourceType.MANUAL)
                .build();
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
}
