package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CR-079 持久化边界守卫（VMD-DSN-CR-079 / D079-06）
 * <p>
 * 本 CR 明确「默认无 DDL」：不新增表/列/索引、不修改 V72 或任何已应用 Flyway，
 * 只向既有 {@code tb_veh_lifecycle} 写事实并更新激活投影 {@code vin_resolved}。
 * 因此：不存在 cr079 命名的新迁移脚本；既有 {@code uk_vin_node(vin,node)} 唯一键可复用。
 *
 * @author hwyz_leo
 * @since 2026-11-25
 */
@DisplayName("CR-079 无 DDL 与生命周期唯一键守卫")
class Cr079LifecycleNoDdlTest {

    private static final String MIGRATION_PATH = "src/main/resources/db/migration";

    @Test
    @DisplayName("不存在 CR-079 新增迁移脚本（本 CR 无 DDL）")
    void noCr079Migration() throws IOException {
        Path migrationDir = Paths.get(MIGRATION_PATH);
        assertTrue(Files.isDirectory(migrationDir), "迁移目录应存在");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(migrationDir, "*.sql")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString().toLowerCase();
                assertFalse(fileName.contains("cr079"),
                        "CR-079 不应新增迁移脚本: " + fileName);
            }
        }
    }

    @Test
    @DisplayName("既有 tb_veh_lifecycle 复用 uk_vin_node(vin,node)，无需新索引")
    void lifecycleUniqueKeyReused() throws IOException {
        String baseline = Files.readString(Paths.get(MIGRATION_PATH, "V0__Baseline.sql"));
        assertTrue(baseline.contains("tb_veh_lifecycle"), "基线应包含车辆生命周期表");
        assertTrue(baseline.contains("uk_vin_node"),
                "既有唯一键 uk_vin_node(vin,node) 是 VEHICLE_ACTIVE 幂等的最终防线");
    }

    @Test
    @DisplayName("V72 激活表结构未被本 CR 修改（保持 CR-077 原样）")
    void cr077MigrationUntouched() throws IOException {
        String v72 = Files.readString(
                Paths.get(MIGRATION_PATH, "V72__cr077_create_veh_activation_tables.sql"));
        assertTrue(v72.contains("tb_veh_activation_node"), "投影表保持不变");
        assertTrue(v72.contains("vin_resolved"), "vin_resolved 标记已有，本 CR 只更新其值");
        assertFalse(v72.contains("ALTER TABLE `tb_veh_lifecycle`"),
                "CR-079 不修改 V72，也不通过迁移变更生命周期表");
    }
}
