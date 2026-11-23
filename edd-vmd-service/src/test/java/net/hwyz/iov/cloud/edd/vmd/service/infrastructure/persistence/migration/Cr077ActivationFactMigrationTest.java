package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.persistence.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * V72 迁移脚本内容校验（VMD-DSN-CR-077 / 持久化迁移批次 078，R-19）
 * <p>
 * 校验：纯新增（expand-only）、两表结构、双重幂等唯一键、投影唯一键、
 * {@code vin varchar(20)}（R-12）、台账 status 注释不含 CONFLICT 终态（R-10）。
 *
 * @author hwyz_leo
 */
@DisplayName("V72 首次激活事实迁移脚本测试（CR-077）")
class Cr077ActivationFactMigrationTest {

    private static final String MIGRATION_PATH = "src/main/resources/db/migration";
    private static final Path SCRIPT =
            Paths.get(MIGRATION_PATH, "V72__cr077_create_veh_activation_tables.sql");

    @Test
    @DisplayName("V72 迁移脚本存在（紧接 V71 不跳号，批次 078）")
    void migrationExists() {
        assertTrue(Files.exists(SCRIPT), "V72 迁移脚本应存在");
    }

    @Test
    @DisplayName("创建两张表：投影 + 台账，且为 expand-only（不改既有表）")
    void createsTwoTablesExpandOnly() throws IOException {
        String content = Files.readString(SCRIPT);
        assertTrue(content.contains("tb_veh_activation_node"), "应创建首次激活节点投影表");
        assertTrue(content.contains("tb_veh_activation_fact_consume"), "应创建消费审计与幂等台账表");
        assertTrue(content.contains("CREATE TABLE IF NOT EXISTS"), "应为幂等建表");
        assertTrue(content.contains("`vin`                  varchar(20)"),
                "vin 列宽必须为 varchar(20)（R-12，与 VMD 既有全表一致）");
        assertTrue(!content.contains("ALTER TABLE"), "不得修改任何既有表（expand-only）");
        assertTrue(!content.contains("DROP TABLE"), "不得删除既有表");
    }

    @Test
    @DisplayName("投影表：vin 唯一键 + 首次激活时刻/来源事件证据字段")
    void nodeTableKeys() throws IOException {
        String content = Files.readString(SCRIPT);
        assertTrue(content.contains("uk_veh_activation_node_vin"), "投影表必须有 vin 唯一键");
        assertTrue(content.contains("first_activated_at"), "投影表必须有首次激活绝对时刻");
        assertTrue(content.contains("payload_digest"), "投影表必须有载荷摘要");
        assertTrue(content.contains("digest_source"), "投影表必须有摘要来源（EVENT/LOCAL）");
        assertTrue(content.contains("vin_resolved"), "投影表必须有 VIN 建档标记");
        assertTrue(content.contains("idx_veh_activation_node_event"), "投影表应按 event_id 建索引");
    }

    @Test
    @DisplayName("台账表：eventId 与 (vin,bindingOperationId) 双重唯一键兜底 + 位点字段")
    void consumeTableKeys() throws IOException {
        String content = Files.readString(SCRIPT);
        assertTrue(content.contains("uk_veh_act_consume_event"), "台账必须有 event_id 唯一键");
        assertTrue(content.contains("uk_veh_act_consume_business"), "台账必须有 (vin, binding_operation_id) 唯一键");
        assertTrue(content.contains("`record_offset`"), "台账必须保留原始位点用于重放");
        assertTrue(content.contains("`partition_no`"), "台账必须保留原始分区");
        assertTrue(content.contains("idx_veh_act_consume_status"), "台账应按状态建索引（FAILED 重放扫描）");
    }

    @Test
    @DisplayName("终态集合不含 CONFLICT（R-10：摘要差异不判冲突终态）")
    void statusWithoutConflict() throws IOException {
        String content = Files.readString(SCRIPT);
        assertTrue(content.contains("RECORDED/DUPLICATE/QUARANTINED/FAILED"));
        assertTrue(!content.contains("CONFLICT/") && !content.contains("/CONFLICT")
                        && !content.contains("CONFLICT,"),
                "R-10 已取消 CONFLICT 终态，不得作为 status 取值出现");
    }

}
