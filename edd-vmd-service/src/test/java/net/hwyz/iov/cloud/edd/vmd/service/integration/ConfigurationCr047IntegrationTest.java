package net.hwyz.iov.cloud.edd.vmd.service.integration;

import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ConfigurationProjectionCommand;
import net.hwyz.iov.cloud.edd.vmd.service.application.mapper.MdmConfigurationProjectionMapper;
import net.hwyz.iov.cloud.edd.vmd.service.application.event.event.MdmConfigurationEvent;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.ConfigurationAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ConfigurationDto;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.ConfigurationProjectionException;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmConfigurationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VMD-DSN-CR-047 集成测试
 * <p>
 * 覆盖：V48 迁移后的目标 schema、最小投影 upsert 内核（空库 Bootstrap 故障回归）、
 * 版本门禁、删除/失效事件逻辑删除、产品树补全查询（完整/缺失上层投影降级）。
 * </p>
 *
 * @author CR-047
 */
@Rollback
class ConfigurationCr047IntegrationTest extends BaseTest {

    @Autowired
    private MdmConfigurationProjectionMapper mdmConfigurationProjectionMapper;

    @Autowired
    private MdmConfigurationRepository mdmConfigurationRepository;

    @Autowired
    private ConfigurationAppService configurationAppService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM tb_mdm_configuration_option_code WHERE configuration_code LIKE 'CR047_%'");
        jdbcTemplate.execute("DELETE FROM tb_mdm_configuration WHERE code LIKE 'CR047_%'");
        jdbcTemplate.execute("DELETE FROM tb_mdm_brand WHERE code LIKE 'CR047_%'");
        jdbcTemplate.execute("DELETE FROM tb_mdm_car_line WHERE code LIKE 'CR047_%'");
        jdbcTemplate.execute("DELETE FROM tb_mdm_platform WHERE code LIKE 'CR047_%'");
        jdbcTemplate.execute("DELETE FROM tb_mdm_model WHERE code LIKE 'CR047_%'");
        jdbcTemplate.execute("DELETE FROM tb_mdm_variant WHERE code LIKE 'CR047_%'");
    }

    @Test
    @DisplayName("V48迁移后 tb_mdm_configuration 应包含 name_local 且不含旧层级冗余列")
    void schema_shouldAlignToMinimalProjection() {
        // Given
        String[] oldColumns = {"platform_code", "car_line_code", "model_code", "vehicle_stage_code", "enable", "sort", "name_en"};

        // When
        for (String column : oldColumns) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() " +
                            "AND table_name = 'tb_mdm_configuration' AND column_name = ?",
                    Integer.class, column);
            assertEquals(0, count, "旧层级冗余列不应存在: " + column);
        }
        Integer nameLocalCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() " +
                        "AND table_name = 'tb_mdm_configuration' AND column_name = 'name_local'",
                Integer.class);
        assertEquals(1, nameLocalCount, "name_local 列应存在");
    }

    @Test
    @DisplayName("最小投影命令 upsert 应成功（回归：空库 Bootstrap 不再报 platform_code cannot be null）")
    void projectionMapper_shouldUpsertWithMinimalPayload() {
        // Given
        ConfigurationProjectionCommand command = ConfigurationProjectionCommand.builder()
                .code("CR047_CFG_001")
                .name("最小投影配置")
                .nameLocal("最小投影配置本地化")
                .variantCode("CR047_VAR_001")
                .description("仅含新字段")
                .externalRefId("cr047-ext-001")
                .externalVersion(1L)
                .occurredAt(LocalDateTime.now())
                .build();

        // When
        mdmConfigurationProjectionMapper.apply(command);

        // Then
        var inserted = mdmConfigurationRepository.selectByCode("CR047_CFG_001");
        assertNotNull(inserted);
        assertEquals("最小投影配置", inserted.getName());
        assertEquals("最小投影配置本地化", inserted.getNameLocal());
        assertEquals("CR047_VAR_001", inserted.getVariantCode());
        assertEquals(SourceType.MDM, inserted.getSource());
        assertEquals(1L, inserted.getExternalVersion());
    }

    @Test
    @DisplayName("同版本重放应幂等（不覆盖、不新增）")
    void projectionMapper_shouldBeIdempotentOnSameVersion() {
        // Given
        ConfigurationProjectionCommand command = ConfigurationProjectionCommand.builder()
                .code("CR047_CFG_002")
                .name("配置")
                .variantCode("CR047_VAR_001")
                .externalRefId("cr047-ext-002")
                .externalVersion(1L)
                .build();
        mdmConfigurationProjectionMapper.apply(command);

        // When（同版本重放）
        mdmConfigurationProjectionMapper.apply(command);

        // Then
        assertEquals(1, mdmConfigurationRepository.countByMap(java.util.Map.of("code", "CR047_CFG_002")));
        var local = mdmConfigurationRepository.selectByCode("CR047_CFG_002");
        assertEquals(1L, local.getExternalVersion());
    }

    @Test
    @DisplayName("更高版本事件应更新投影")
    void projectionMapper_shouldUpdateOnHigherVersion() {
        // Given
        ConfigurationProjectionCommand command = ConfigurationProjectionCommand.builder()
                .code("CR047_CFG_003")
                .name("旧名称")
                .variantCode("CR047_VAR_001")
                .externalRefId("cr047-ext-003")
                .externalVersion(1L)
                .build();
        mdmConfigurationProjectionMapper.apply(command);

        ConfigurationProjectionCommand update = ConfigurationProjectionCommand.builder()
                .code("CR047_CFG_003")
                .name("新名称")
                .nameLocal("新本地化")
                .variantCode("CR047_VAR_001")
                .externalRefId("cr047-ext-003")
                .externalVersion(2L)
                .build();

        // When
        mdmConfigurationProjectionMapper.apply(update);

        // Then
        var local = mdmConfigurationRepository.selectByCode("CR047_CFG_003");
        assertEquals("新名称", local.getName());
        assertEquals("新本地化", local.getNameLocal());
        assertEquals(2L, local.getExternalVersion());
    }

    @Test
    @DisplayName("缺失 variantCode 的 payload 应抛契约错误且不写半条投影")
    void projectionMapper_shouldRejectPayloadMissingVariantCode() {
        // Given
        ConfigurationProjectionCommand command = ConfigurationProjectionCommand.builder()
                .code("CR047_CFG_004")
                .name("配置")
                .externalRefId("cr047-ext-004")
                .externalVersion(1L)
                .build();

        // When
        assertThrows(ConfigurationProjectionException.class, () -> mdmConfigurationProjectionMapper.apply(command));

        // Then
        assertNull(mdmConfigurationRepository.selectByCode("CR047_CFG_004"));
    }

    @Test
    @DisplayName("删除/失效事件应逻辑删除投影")
    void projectionMapper_shouldLogicalDeleteOnDeactivatedEvent() {
        // Given
        ConfigurationProjectionCommand command = ConfigurationProjectionCommand.builder()
                .code("CR047_CFG_005")
                .name("配置")
                .variantCode("CR047_VAR_001")
                .externalRefId("cr047-ext-005")
                .externalVersion(1L)
                .build();
        mdmConfigurationProjectionMapper.apply(command);

        MdmConfigurationEvent deactivateEvent = new MdmConfigurationEvent("DEACTIVATED", "cr047-ext-005", 3L, "CR047_CFG_005",
                null, null, null, null, LocalDateTime.now());

        // When
        mdmConfigurationProjectionMapper.handleDeletion(deactivateEvent);

        // Then
        assertNull(mdmConfigurationRepository.selectByCode("CR047_CFG_005"));
    }

    @Test
    @DisplayName("getConfigurationByCode 应沿产品树补全层级字段")
    void getConfigurationByCode_shouldCompleteHierarchyFromProductTree() {
        // Given
        jdbcTemplate.execute("INSERT INTO tb_mdm_brand (code, name, enable, sort, source, row_valid) VALUES ('CR047_BRAND', '品牌', 1, 1, 'MANUAL', 1)");
        jdbcTemplate.execute("INSERT INTO tb_mdm_platform (code, name, enable, sort, source, row_valid) VALUES ('CR047_PLATFORM', '平台', 1, 1, 'MANUAL', 1)");
        jdbcTemplate.execute("INSERT INTO tb_mdm_car_line (code, name, brand_code, enable, sort, source, row_valid) VALUES ('CR047_CARLINE', '车系', 'CR047_BRAND', 1, 1, 'MANUAL', 1)");
        jdbcTemplate.execute("INSERT INTO tb_mdm_model (code, name, platform_code, car_line_code, enable, sort, source, row_valid) VALUES ('CR047_MODEL', '车型', 'CR047_PLATFORM', 'CR047_CARLINE', 1, 1, 'MANUAL', 1)");
        jdbcTemplate.execute("INSERT INTO tb_mdm_variant (code, name, platform_code, car_line_code, model_code, enable, sort, source, row_valid) VALUES ('CR047_VAR_001', '版本', 'CR047_PLATFORM', 'CR047_CARLINE', 'CR047_MODEL', 1, 1, 'MANUAL', 1)");
        mdmConfigurationProjectionMapper.apply(ConfigurationProjectionCommand.builder()
                .code("CR047_CFG_006").name("配置").variantCode("CR047_VAR_001")
                .externalRefId("cr047-ext-006").externalVersion(1L).build());

        // When
        ConfigurationDto dto = configurationAppService.getConfigurationByCode("CR047_CFG_006");

        // Then
        assertNotNull(dto);
        assertEquals("CR047_CFG_006", dto.getCode());
        assertEquals("CR047_VAR_001", dto.getVariantCode());
        assertEquals("CR047_MODEL", dto.getModelCode());
        assertEquals("CR047_CARLINE", dto.getCarLineCode());
        assertEquals("CR047_PLATFORM", dto.getPlatformCode());
        assertEquals("CR047_BRAND", dto.getBrandCode());
    }

    @Test
    @DisplayName("getConfigurationByCode 在上层投影缺失时应降级返回基础信息（派生字段为null）")
    void getConfigurationByCode_shouldDegradeWhenUpperProjectionMissing() {
        // Given
        mdmConfigurationProjectionMapper.apply(ConfigurationProjectionCommand.builder()
                .code("CR047_CFG_007").name("无上层配置").variantCode("CR047_VAR_NOT_EXIST")
                .externalRefId("cr047-ext-007").externalVersion(1L).build());

        // When
        ConfigurationDto dto = configurationAppService.getConfigurationByCode("CR047_CFG_007");

        // Then
        assertNotNull(dto);
        assertEquals("无上层配置", dto.getName());
        assertNull(dto.getModelCode());
        assertNull(dto.getCarLineCode());
        assertNull(dto.getPlatformCode());
        assertNull(dto.getBrandCode());
    }

    @Test
    @DisplayName("按 variantCode 查询应直接命中并批量补全")
    void getConfigurationListByVariantCode_shouldReturnHierarchyEnrichedList() {
        // Given
        mdmConfigurationProjectionMapper.apply(ConfigurationProjectionCommand.builder()
                .code("CR047_CFG_008").name("配置A").variantCode("CR047_VAR_001")
                .externalRefId("cr047-ext-008").externalVersion(1L).build());
        mdmConfigurationProjectionMapper.apply(ConfigurationProjectionCommand.builder()
                .code("CR047_CFG_009").name("配置B").variantCode("CR047_VAR_001")
                .externalRefId("cr047-ext-009").externalVersion(1L).build());

        // When
        List<ConfigurationDto> dtoList = configurationAppService.getConfigurationListByVariantCode("CR047_VAR_001");

        // Then
        assertNotNull(dtoList);
        assertTrue(dtoList.stream().anyMatch(d -> "CR047_CFG_008".equals(d.getCode())));
        assertTrue(dtoList.stream().anyMatch(d -> "CR047_CFG_009".equals(d.getCode())));
    }
}
