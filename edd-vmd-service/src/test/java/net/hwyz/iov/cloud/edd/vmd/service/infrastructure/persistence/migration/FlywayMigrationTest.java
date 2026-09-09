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
}