package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.migration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Flyway迁移脚本测试
 *
 * @author hwyz_leo
 */
class FlywayMigrationTest {

    private static final String MIGRATION_PATH = "src/main/resources/db/migration";

    @Test
    @DisplayName("V58迁移脚本文件应存在（CR-058）")
    void v58MigrationScript_shouldExist() {
        Path migrationFile = Paths.get(MIGRATION_PATH, "V58__Create_business_key_provisioning_cr058.sql");
        assertTrue(Files.exists(migrationFile), "V58迁移脚本文件应该存在");
    }

    @Test
    @DisplayName("V58迁移脚本应创建开通批次与ACK明细表并调整目录唯一键（CR-058）")
    void v58MigrationScript_shouldCreateProvisioningTables() throws IOException {
        Path migrationFile = Paths.get(MIGRATION_PATH, "V58__Create_business_key_provisioning_cr058.sql");
        String content = Files.readString(migrationFile);
        assertTrue(content.contains("tb_business_key_provisioning"), "应创建开通批次表");
        assertTrue(content.contains("tb_business_key_provisioning_item"), "应创建ACK明细表");
        assertTrue(content.contains("uk_request_id"), "批次表应有requestId唯一约束");
        assertTrue(content.contains("open_flag"), "批次表应有未终结生成列");
        assertTrue(content.contains("uk_device_domain_open"), "批次表应有同设备+域开放唯一约束");
        assertTrue(content.contains("uk_provisioning_purpose"), "明细表应有批次+用途唯一约束");
        assertTrue(content.contains("uk_request_purpose"), "目录表应调整requestId+purpose唯一约束");
        assertTrue(content.contains("DROP INDEX `uk_request_id`"), "目录表应删除旧requestId唯一约束");
    }

    @Test
    @DisplayName("V5迁移脚本文件应存在")
    void v5MigrationScript_shouldExist() {
        // Given
        Path migrationFile = Paths.get(MIGRATION_PATH, "V5__Migrate_manufacturer_to_plant.sql");

        // Then
        assertTrue(Files.exists(migrationFile), "V5迁移脚本文件应该存在");
    }

    @Test
    @DisplayName("V5迁移脚本应包含RENAME TABLE语句")
    void v5MigrationScript_shouldContainRenameTable() throws IOException {
        // Given
        Path migrationFile = Paths.get(MIGRATION_PATH, "V5__Migrate_manufacturer_to_plant.sql");
        String content = Files.readString(migrationFile);

        // Then
        assertTrue(content.contains("RENAME TABLE"), "迁移脚本应包含RENAME TABLE语句");
        assertTrue(content.contains("tb_veh_manufacturer"), "迁移脚本应包含源表名");
        assertTrue(content.contains("tb_veh_plant"), "迁移脚本应包含目标表名");
    }

    @Test
    @DisplayName("V5迁移脚本应包含列重命名语句")
    void v5MigrationScript_shouldContainColumnRename() throws IOException {
        // Given
        Path migrationFile = Paths.get(MIGRATION_PATH, "V5__Migrate_manufacturer_to_plant.sql");
        String content = Files.readString(migrationFile);

        // Then
        assertTrue(content.contains("CHANGE COLUMN"), "迁移脚本应包含CHANGE COLUMN语句");
        assertTrue(content.contains("plant_code"), "迁移脚本应包含plant_code列");
        assertTrue(content.contains("plant_name"), "迁移脚本应包含plant_name列");
    }

    @Test
    @DisplayName("V5迁移脚本应包含MDM投影字段")
    void v5MigrationScript_shouldContainMdmProjectionFields() throws IOException {
        // Given
        Path migrationFile = Paths.get(MIGRATION_PATH, "V5__Migrate_manufacturer_to_plant.sql");
        String content = Files.readString(migrationFile);

        // Then
        assertTrue(content.contains("source"), "迁移脚本应包含source字段");
        assertTrue(content.contains("external_ref_id"), "迁移脚本应包含external_ref_id字段");
        assertTrue(content.contains("external_version"), "迁移脚本应包含external_version字段");
        assertTrue(content.contains("last_sync_time"), "迁移脚本应包含last_sync_time字段");
    }

    @Test
    @DisplayName("V5迁移脚本应包含plant_code字段添加")
    void v5MigrationScript_shouldContainPlantCodeColumnAddition() throws IOException {
        // Given
        Path migrationFile = Paths.get(MIGRATION_PATH, "V5__Migrate_manufacturer_to_plant.sql");
        String content = Files.readString(migrationFile);

        // Then
        assertTrue(content.contains("ALTER TABLE `tb_veh_basic_info`"), "迁移脚本应包含对tb_veh_basic_info表的修改");
        assertTrue(content.contains("ADD COLUMN `plant_code`"), "迁移脚本应包含添加plant_code列");
    }

    @Test
    @DisplayName("V5迁移脚本应包含数据回填语句")
    void v5MigrationScript_shouldContainDataBackfill() throws IOException {
        // Given
        Path migrationFile = Paths.get(MIGRATION_PATH, "V5__Migrate_manufacturer_to_plant.sql");
        String content = Files.readString(migrationFile);

        // Then
        assertTrue(content.contains("UPDATE `tb_veh_basic_info`"), "迁移脚本应包含UPDATE语句");
        assertTrue(content.contains("SET `plant_code` = `manufacturer_code`"), "迁移脚本应包含数据回填逻辑");
    }

    @Test
    @DisplayName("V5迁移脚本应包含唯一约束")
    void v5MigrationScript_shouldContainUniqueConstraint() throws IOException {
        // Given
        Path migrationFile = Paths.get(MIGRATION_PATH, "V5__Migrate_manufacturer_to_plant.sql");
        String content = Files.readString(migrationFile);

        // Then
        assertTrue(content.contains("UNIQUE KEY"), "迁移脚本应包含唯一约束");
        assertTrue(content.contains("uk_plant_code"), "迁移脚本应包含plant_code唯一约束");
        assertTrue(content.contains("uk_external_ref_id"), "迁移脚本应包含external_ref_id唯一约束");
    }

    @Test
    @DisplayName("V5迁移脚本应包含表注释更新")
    void v5MigrationScript_shouldContainTableCommentUpdate() throws IOException {
        // Given
        Path migrationFile = Paths.get(MIGRATION_PATH, "V5__Migrate_manufacturer_to_plant.sql");
        String content = Files.readString(migrationFile);

        // Then
        assertTrue(content.contains("COMMENT"), "迁移脚本应包含COMMENT语句");
        assertTrue(content.contains("车辆生产工厂表"), "迁移脚本应包含更新后的表注释");
    }

    @Test
    @DisplayName("V6迁移脚本文件应存在")
    void v6MigrationScript_shouldExist() {
        // Given
        Path migrationFile = Paths.get(MIGRATION_PATH, "V6__Add_mdm_source_to_model.sql");

        // Then
        assertTrue(Files.exists(migrationFile), "V6迁移脚本文件应该存在");
    }

    @Test
    @DisplayName("V6迁移脚本应包含MDM投影字段")
    void v6MigrationScript_shouldContainMdmProjectionFields() throws IOException {
        // Given
        Path migrationFile = Paths.get(MIGRATION_PATH, "V6__Add_mdm_source_to_model.sql");
        String content = Files.readString(migrationFile);

        // Then
        assertTrue(content.contains("source"), "迁移脚本应包含source字段");
        assertTrue(content.contains("external_ref_id"), "迁移脚本应包含external_ref_id字段");
        assertTrue(content.contains("external_version"), "迁移脚本应包含external_version字段");
        assertTrue(content.contains("last_sync_time"), "迁移脚本应包含last_sync_time字段");
    }

    @Test
    @DisplayName("V6迁移脚本应包含唯一约束")
    void v6MigrationScript_shouldContainUniqueConstraint() throws IOException {
        // Given
        Path migrationFile = Paths.get(MIGRATION_PATH, "V6__Add_mdm_source_to_model.sql");
        String content = Files.readString(migrationFile);

        // Then
        assertTrue(content.contains("UNIQUE KEY"), "迁移脚本应包含唯一约束");
        assertTrue(content.contains("uk_external_ref_id"), "迁移脚本应包含external_ref_id唯一约束");
    }

    @Test
    @DisplayName("V6迁移脚本应包含数据回填语句")
    void v6MigrationScript_shouldContainDataBackfill() throws IOException {
        // Given
        Path migrationFile = Paths.get(MIGRATION_PATH, "V6__Add_mdm_source_to_model.sql");
        String content = Files.readString(migrationFile);

        // Then
        assertTrue(content.contains("UPDATE"), "迁移脚本应包含UPDATE语句");
        assertTrue(content.contains("source"), "迁移脚本应回填source字段");
    }

    @Test
    @DisplayName("V6迁移脚本应针对veh_model表")
    void v6MigrationScript_shouldTargetVehModelTable() throws IOException {
        // Given
        Path migrationFile = Paths.get(MIGRATION_PATH, "V6__Add_mdm_source_to_model.sql");
        String content = Files.readString(migrationFile);

        // Then
        assertTrue(content.contains("veh_model"), "迁移脚本应针对veh_model表");
    }

    @Test
    @DisplayName("所有迁移脚本文件应存在")
    void allMigrationScripts_shouldExist() {
        // Given
        String[] expectedFiles = {
                "V0__Baseline.sql",
                "V1__BuildConfig_feature_code_migration.sql",
                "V2__Series_brand_code_migration.sql",
                "V3__Add_mdm_source_to_product_tree.sql",
                "V4__Rename_series_to_car_line.sql",
                "V5__Migrate_manufacturer_to_plant.sql",
                "V6__Add_mdm_source_to_model.sql"
        };

        // Then
        for (String fileName : expectedFiles) {
            Path migrationFile = Paths.get(MIGRATION_PATH, fileName);
            assertTrue(Files.exists(migrationFile), "迁移脚本文件应该存在: " + fileName);
        }
    }

    @Test
    @DisplayName("V47迁移脚本文件应存在（CR-046）")
    void v47MigrationScript_shouldExist() {
        Path migrationFile = Paths.get(MIGRATION_PATH, "V47__Alter_part_software_installation_cr046.sql");
        assertTrue(Files.exists(migrationFile), "V47迁移脚本文件应该存在");
    }

    @Test
    @DisplayName("V47迁移脚本应包含part_software_installation新列（CR-046）")
    void v47MigrationScript_shouldContainNewColumns() throws IOException {
        Path migrationFile = Paths.get(MIGRATION_PATH, "V47__Alter_part_software_installation_cr046.sql");
        String content = Files.readString(migrationFile);
        assertTrue(content.contains("is_active_slot"), "迁移脚本应包含is_active_slot列");
        assertTrue(content.contains("observation_key"), "迁移脚本应包含observation_key列");
        assertTrue(content.contains("canonicalization_version"), "迁移脚本应包含canonicalization_version列");
        assertTrue(content.contains("canonical_digest"), "迁移脚本应包含canonical_digest列");
        assertTrue(content.contains("source_accepted_at"), "迁移脚本应包含source_accepted_at列");
    }

    @Test
    @DisplayName("V47迁移脚本应创建消费审计表（CR-046）")
    void v47MigrationScript_shouldCreateConsumeAuditTable() throws IOException {
        Path migrationFile = Paths.get(MIGRATION_PATH, "V47__Alter_part_software_installation_cr046.sql");
        String content = Files.readString(migrationFile);
        assertTrue(content.contains("tb_software_inventory_consume_audit"), "迁移脚本应创建消费审计表");
        assertTrue(content.contains("uk_event_id"), "消费审计表应包含event_id唯一约束");
        assertTrue(content.contains("uk_observation_key"), "消费审计表应包含observation_key唯一约束");
        assertTrue(content.contains("item_quarantined"), "消费审计表应包含隔离统计列");
    }

    @Test
    @DisplayName("V48迁移脚本文件应存在（CR-047）")
    void v48MigrationScript_shouldExist() {
        Path migrationFile = Paths.get(MIGRATION_PATH, "V48__cr047_configuration_projection_alignment.sql");
        assertTrue(Files.exists(migrationFile), "V48迁移脚本文件应该存在");
    }

    @Test
    @DisplayName("V48迁移脚本应新增name_local并删除旧层级冗余列（CR-047）")
    void v48MigrationScript_shouldAlignConfigurationProjection() throws IOException {
        Path migrationFile = Paths.get(MIGRATION_PATH, "V48__cr047_configuration_projection_alignment.sql");
        String content = Files.readString(migrationFile);
        assertTrue(content.contains("name_local"), "迁移脚本应新增name_local列");
        assertTrue(content.contains("variant_code"), "迁移脚本应收敛variant_code为NOT NULL");
        assertTrue(content.contains("DROP COLUMN `platform_code`"), "迁移脚本应删除platform_code列");
        assertTrue(content.contains("DROP COLUMN `car_line_code`"), "迁移脚本应删除car_line_code列");
        assertTrue(content.contains("DROP COLUMN `model_code`"), "迁移脚本应删除model_code列");
        assertTrue(content.contains("DROP COLUMN `vehicle_stage_code`"), "迁移脚本应删除vehicle_stage_code列");
        assertTrue(content.contains("DROP COLUMN `enable`"), "迁移脚本应删除enable列");
        assertTrue(content.contains("DROP COLUMN `sort`"), "迁移脚本应删除sort列");
        assertTrue(content.contains("DROP COLUMN `name_en`"), "迁移脚本应删除name_en列");
    }

    @Test
    @DisplayName("V48迁移脚本应新增variant查询索引（CR-047）")
    void v48MigrationScript_shouldAddVariantIndex() throws IOException {
        Path migrationFile = Paths.get(MIGRATION_PATH, "V48__cr047_configuration_projection_alignment.sql");
        String content = Files.readString(migrationFile);
        assertTrue(content.contains("idx_mdm_configuration_variant"), "迁移脚本应新增variant查询索引");
        assertTrue(content.contains("idx_mdm_configuration_sync"), "迁移脚本应新增同步监控索引");
    }

    @Test
    @DisplayName("V63迁移脚本文件应存在（CR-066）")
    void v63MigrationScript_shouldExist() {
        Path migrationFile = Paths.get(MIGRATION_PATH, "V63__Alter_business_key_cr066.sql");
        assertTrue(Files.exists(migrationFile), "V63迁移脚本文件应该存在");
    }

    @Test
    @DisplayName("V63迁移脚本应增加 claim-or-reuse 辅助字段与索引并保留 uk_key_id（CR-066）")
    void v63MigrationScript_shouldAddClaimSupportColumns() throws IOException {
        Path migrationFile = Paths.get(MIGRATION_PATH, "V63__Alter_business_key_cr066.sql");
        String content = Files.readString(migrationFile);
        assertTrue(content.contains("superseded_by_id"), "应新增 superseded_by_id 列");
        assertTrue(content.contains("logical_identity_digest"), "应新增 logical_identity_digest 列");
        assertTrue(content.contains("recovery_count"), "应新增 recovery_count 列");
        assertTrue(content.contains("last_recovery_request_id"), "应新增 last_recovery_request_id 列");
        assertTrue(content.contains("idx_logical_identity_digest"), "应新增 logical_identity_digest 索引");
        assertTrue(content.contains("idx_superseded_by_id"), "应新增 superseded_by_id 索引");
        assertFalse(content.contains("DROP INDEX"), "不得删除既有约束（uk_key_id 保留）");
    }

    @Test
    @DisplayName("V64迁移脚本文件应存在（CR-068）")
    void v64MigrationScript_shouldExist() {
        Path migrationFile = Paths.get(MIGRATION_PATH, "V64__Alter_business_key_provisioning_cr068.sql");
        assertTrue(Files.exists(migrationFile), "V64迁移脚本文件应该存在");
    }

    @Test
    @DisplayName("V64迁移脚本应显式拆分 core/wire 摘要并新增批次级 Response 重放字段（CR-068）")
    void v64MigrationScript_shouldSplitDigestsAndAddResponseStore() throws IOException {
        Path migrationFile = Paths.get(MIGRATION_PATH, "V64__Alter_business_key_provisioning_cr068.sql");
        String content = Files.readString(migrationFile);
        // item 表：envelope_digest 显式 RENAME → core_envelope_digest（不得静默把历史 core 值解释为 wire 值）
        assertTrue(content.contains("RENAME COLUMN `envelope_digest` TO `core_envelope_digest`"),
                "必须显式迁移 envelope_digest 语义为 core_envelope_digest");
        assertTrue(content.contains("`wire_envelope_digest`"), "应新增 wire_envelope_digest 列");
        assertTrue(content.contains("`aad_profile_id`"), "应新增 aad_profile_id 列");
        assertTrue(content.contains("`parameters_digest`"), "应新增 parameters_digest 列");
        assertTrue(content.contains("`wrapped_key_digest`"), "应新增 wrapped_key_digest 列");
        // 批次表：Response 字节级重放（幂等键=requestId，存储粒度与幂等键一致）
        assertTrue(content.contains("`response_payload`"), "应新增 response_payload（加密 BLOB）列");
        assertTrue(content.contains("`response_ref`"), "应新增 response_ref 列");
        assertTrue(content.contains("`response_digest`"), "应新增 response_digest 列");
        assertTrue(content.contains("`response_codec_version`"), "应新增 response_codec_version 列");
        assertTrue(content.contains("`response_persisted_at`"), "应新增 response_persisted_at 列");
        // item 表不得承载最终 Response 本体（存储粒度=批次表）：提取 item ALTER 段精确断言
        int itemAlterStart = content.indexOf("ALTER TABLE `tb_business_key_provisioning_item`");
        int batchAlterStart = content.indexOf("ALTER TABLE `tb_business_key_provisioning`", itemAlterStart);
        assertTrue(itemAlterStart >= 0 && batchAlterStart > itemAlterStart, "迁移脚本应包含两段 ALTER");
        String itemAlterSegment = content.substring(itemAlterStart, batchAlterStart);
        assertFalse(itemAlterSegment.contains("`response_payload`"), "Response 本体不得落在 item 表（批次表承载）");
    }

    @Test
    @DisplayName("V70迁移脚本应存在并添加 device_category 可空列（CR-072）")
    void v70MigrationScript_shouldAddDeviceCategory() throws IOException {
        Path migrationFile = Paths.get(MIGRATION_PATH, "V70__cr072_add_part_info_device_category.sql");
        assertTrue(Files.exists(migrationFile), "V70迁移脚本文件应该存在");
        String content = Files.readString(migrationFile);
        assertTrue(content.contains("ALTER TABLE `tb_part_info`"), "应修改 tb_part_info");
        assertTrue(content.contains("device_category"), "应新增 device_category 列");
        assertTrue(content.contains("VARCHAR(20)"), "列长应对齐节点投影 VARCHAR(20)");
        assertTrue(content.contains("NULL DEFAULT NULL"), "应为可空列");
        assertTrue(!content.contains("DROP TABLE"), "expand-only：不应含 DROP TABLE");
        assertTrue(!content.contains("CREATE INDEX"), "首版不建索引（CR-072）");
    }
}
