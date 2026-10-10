package net.hwyz.iov.cloud.edd.vmd.service.integration;

import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ReplayVehicleImportEventCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ReplayActionResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ReplayEventResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.VehicleImportReplayPreview;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.VehImportEventReplayAppService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VMD-DSN-CR-057 集成测试
 * <p>
 * 覆盖：车辆导入补发按 ImportType 路由的动作补偿完整链路——TOL/EOL 生命周期节点幂等补齐、
 * 绑定/软件实装事件经 vmd_outbox 重放（含 replay 元数据）、逐项动作审计、replayId 幂等、
 * preview 预检、主体零修改、证书/密钥/安全预置副作用零触发、PRODUCE 兼容回归。
 * </p>
 *
 * @author CR-057
 */
@Rollback
class VehImportEventReplayCr057IntegrationTest extends BaseTest {

    private static final String VIN = "CR057-VIN001";

    @Autowired
    private VehImportEventReplayAppService replayAppService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM tb_veh_import_event_replay_item WHERE replay_id IN "
                + "(SELECT replay_id FROM tb_veh_import_event_replay WHERE veh_import_data_id IN "
                + "(SELECT id FROM tb_veh_import_data WHERE batch_num LIKE 'CR057-%'))");
        jdbcTemplate.update("DELETE FROM tb_vmd_outbox WHERE source_type = 'IMPORT_EVENT_REPLAY'");
        jdbcTemplate.update("DELETE FROM tb_veh_import_event_replay WHERE veh_import_data_id IN "
                + "(SELECT id FROM tb_veh_import_data WHERE batch_num LIKE 'CR057-%')");
        jdbcTemplate.update("DELETE FROM tb_veh_lifecycle WHERE vin LIKE 'CR057-%'");
        jdbcTemplate.update("DELETE FROM tb_veh_security_constant WHERE vin LIKE 'CR057-%'");
        jdbcTemplate.update("DELETE FROM tb_part_software_installation WHERE binding_id IN "
                + "(SELECT id FROM tb_vehicle_part WHERE vin LIKE 'CR057-%')");
        jdbcTemplate.update("DELETE FROM tb_vehicle_part WHERE vin LIKE 'CR057-%'");
        jdbcTemplate.update("DELETE FROM tb_part_info WHERE part_code LIKE 'CR057-%'");
        jdbcTemplate.update("DELETE FROM tb_veh_basic_info WHERE vin LIKE 'CR057-%'");
        jdbcTemplate.update("DELETE FROM tb_veh_import_data WHERE batch_num LIKE 'CR057-%'");
    }

    /**
     * 准备 TOL 导入记录 + 车辆 + 零件实例 + active 绑定 + 软件实装
     */
    private Long prepareTolImport(String batchNum, String partCode, String sn, boolean withBinding,
                                  boolean withSoftware, boolean withTolNode) {
        String data = "{\"REQUEST\":{\"DATA\":{\"ITEMS\":[{\"VIN\":\"" + VIN + "\",\"PARTS\":["
                + "{\"ASSEMBLY_PART_NO\":\"" + partCode + "\",\"SN\":\"" + sn + "\"}]}]}}}";
        jdbcTemplate.update(
                "INSERT INTO tb_veh_import_data (batch_num, type, version, data, handle) VALUES (?, 'TOL', '1.0', ?, 1)",
                batchNum, data);

        jdbcTemplate.update(
                "INSERT INTO tb_veh_basic_info (vin, plant_code, brand_code, platform_code, car_line_code, "
                        + "model_code, variant_code, configuration_code, order_num) "
                        + "VALUES (?, 'P001', 'B001', 'PL001', 'CL001', 'M001', 'V001', 'C001', 'PO001')",
                VIN);

        jdbcTemplate.update(
                "INSERT INTO tb_part_info (part_code, sn, instance_state, source, part_type) "
                        + "VALUES (?, ?, 1, 'MANUAL', 'OTHER')",
                partCode, sn);

        Long partId = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_part_info WHERE part_code = ? AND sn = ?", Long.class, partCode, sn);

        Long bindingId = null;
        if (withBinding) {
            jdbcTemplate.update(
                    "INSERT INTO tb_vehicle_part (vin, part_id, vehicle_node_code, bind_time, bind_state, bind_by) "
                            + "VALUES (?, ?, 'TBOX_5G', '2026-10-01 00:00:00', 1, 'CR057')",
                    VIN, partId);
            bindingId = jdbcTemplate.queryForObject(
                    "SELECT id FROM tb_vehicle_part WHERE vin = ? AND part_id = ?", Long.class, VIN, partId);
        }
        if (withSoftware && bindingId != null) {
            jdbcTemplate.update(
                    "INSERT INTO tb_part_software_installation "
                            + "(part_id, binding_id, vin_snapshot, software_target_code, software_part_no, "
                            + "software_version, install_state, change_type, source, inventory_version, "
                            + "is_confirmed, is_active_slot) "
                            + "VALUES (?, ?, ?, 'TBOX_APP', 'SPN001', '1.0.0', 'ACTIVE', 'INITIAL', 'EOL', 1, 1, 1)",
                    partId, bindingId, VIN);
        }
        if (withTolNode) {
            jdbcTemplate.update(
                    "INSERT INTO tb_veh_lifecycle (vin, node, reach_time, sort) "
                            + "VALUES (?, 'TOL', '2026-10-01 08:00:00', 1)",
                    VIN);
        }
        return jdbcTemplate.queryForObject(
                "SELECT id FROM tb_veh_import_data WHERE batch_num = ?", Long.class, batchNum);
    }

    /**
     * 准备 EOL 导入记录（含 EOL_TIME）
     */
    private Long prepareEolImport(String batchNum) {
        String data = "{\"REQUEST\":{\"DATA\":{\"ITEMS\":[{\"VIN\":\"" + VIN + "\",\"EOL_TIME\":1784391702000,"
                + "\"PARTS\":[{\"PART_NO\":\"CR057-EOLPN\",\"PART_SN\":\"CR057-EOLSN\"}]}]}}}";
        jdbcTemplate.update(
                "INSERT INTO tb_veh_import_data (batch_num, type, version, data, handle) VALUES (?, 'EOL', '1.0', ?, 1)",
                batchNum, data);
        jdbcTemplate.update(
                "INSERT INTO tb_veh_basic_info (vin, plant_code, brand_code, platform_code, car_line_code, "
                        + "model_code, variant_code, configuration_code, order_num) "
                        + "VALUES (?, 'P001', 'B001', 'PL001', 'CL001', 'M001', 'V001', 'C001', 'PO001')",
                VIN);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM tb_veh_import_data WHERE batch_num = ?", Long.class, batchNum);
    }

    /**
     * 准备 PRODUCE 导入记录
     */
    private Long prepareProduceImport(String batchNum) {
        String data = "{\"REQUEST\":{\"DATA\":{\"ITEMS\":[{\"VIN\":\"" + VIN + "\"}]}}}";
        jdbcTemplate.update(
                "INSERT INTO tb_veh_import_data (batch_num, type, version, data, handle) VALUES (?, 'PRODUCE', '1.0', ?, 1)",
                batchNum, data);
        jdbcTemplate.update(
                "INSERT INTO tb_veh_basic_info (vin, plant_code, brand_code, platform_code, car_line_code, "
                        + "model_code, variant_code, configuration_code, order_num) "
                        + "VALUES (?, 'P001', 'B001', 'PL001', 'CL001', 'M001', 'V001', 'C001', 'PO001')",
                VIN);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM tb_veh_import_data WHERE batch_num = ?", Long.class, batchNum);
    }

    @Test
    @DisplayName("TOL 补发完整链路：生命周期补齐 + 绑定/软件实装事件入 Outbox + 逐项审计，主体零修改")
    void tolReplay_fullFlow() {
        Long id = prepareTolImport("CR057-TOL-001", "CR057-TOLPN", "CR057-TOLSN", true, true, false);

        ReplayEventResult result = replayAppService.replay(
                id, ReplayVehicleImportEventCmd.builder().reason("CR057集成测试").build(), "op-cr057", "CR057-Operator");

        // 动作：TOL_LIFECYCLE_ENSURE(SUCCESS) + BINDING(QUEUED) + SOFTWARE(QUEUED)
        assertEquals(3, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(2, result.getQueuedCount());
        assertEquals(0, result.getSkipCount());
        assertEquals(0, result.getFailureCount());

        // 生命周期节点补齐
        Integer tolNodes = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_veh_lifecycle WHERE vin = ? AND node = 'TOL'", Integer.class, VIN);
        assertEquals(1, tolNodes);

        // 绑定 + 软件实装事件各 1 条写入 Outbox（source_type=IMPORT_EVENT_REPLAY）
        Integer bindingOutbox = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_vmd_outbox WHERE source_type='IMPORT_EVENT_REPLAY' AND source_ref_id=? "
                        + "AND event_type='VehiclePartBindingChangedEvent'", Integer.class, result.getReplayId());
        Integer softwareOutbox = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_vmd_outbox WHERE source_type='IMPORT_EVENT_REPLAY' AND source_ref_id=? "
                        + "AND event_type='VehicleSoftwareInventoryChangedEvent'", Integer.class, result.getReplayId());
        assertEquals(1, bindingOutbox);
        assertEquals(1, softwareOutbox);

        // 补发元数据写入 payload
        String bindingPayload = jdbcTemplate.queryForObject(
                "SELECT payload FROM tb_vmd_outbox WHERE source_type='IMPORT_EVENT_REPLAY' AND source_ref_id=? "
                        + "AND event_type='VehiclePartBindingChangedEvent'", String.class, result.getReplayId());
        assertTrue(bindingPayload.contains("\"replay\":true"));
        assertTrue(bindingPayload.contains("\"replayId\":\"" + result.getReplayId() + "\""));
        assertTrue(bindingPayload.contains("\"batchNum\":\"CR057-TOL-001\""));

        // 逐项动作审计
        Integer itemCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_veh_import_event_replay_item WHERE replay_id = ?", Integer.class, result.getReplayId());
        assertEquals(3, itemCount);
        Integer successItems = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_veh_import_event_replay_item WHERE replay_id = ? AND status='SUCCESS'",
                Integer.class, result.getReplayId());
        Integer queuedItems = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_veh_import_event_replay_item WHERE replay_id = ? AND status='QUEUED'",
                Integer.class, result.getReplayId());
        assertEquals(1, successItems);
        assertEquals(2, queuedItems);

        // 主体零修改：veh_import_data.handle 不变
        Map<String, Object> importRow = jdbcTemplate.queryForMap(
                "SELECT handle FROM tb_veh_import_data WHERE id = ?", id);
        assertEquals(Boolean.TRUE, importRow.get("handle"));

        // 副作用零触发：未写安全常量（证书/密钥/安全预置副作用）
        Integer secCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_veh_security_constant WHERE vin = ?", Integer.class, VIN);
        assertEquals(0, secCount);
    }

    @Test
    @DisplayName("TOL 已有生命周期节点 → 生命周期动作 SKIPPED，主任务 SUCCEEDED_WITH_SKIPS")
    void tolReplay_lifecycleAlreadyExists() {
        Long id = prepareTolImport("CR057-TOL-002", "CR057-TOLPN", "CR057-TOLSN", true, true, true);

        ReplayEventResult result = replayAppService.replay(
                id, ReplayVehicleImportEventCmd.builder().build(), "op-cr057", "CR057-Operator");

        assertEquals(1, result.getSkipCount());
        assertEquals("SUCCEEDED_WITH_SKIPS", jdbcTemplate.queryForObject(
                "SELECT status FROM tb_veh_import_event_replay WHERE replay_id = ?", String.class, result.getReplayId()));

        // 逐项：生命周期 SKIPPED 且原因 ALREADY_EXISTS
        String skipReason = jdbcTemplate.queryForObject(
                "SELECT skip_reason FROM tb_veh_import_event_replay_item WHERE replay_id = ? "
                        + "AND action_type='TOL_LIFECYCLE_ENSURE'", String.class, result.getReplayId());
        assertTrue(skipReason.startsWith("ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("EOL 补发按原批次 EOL_TIME 补齐生命周期节点")
    void eolReplay_usesBatchTime() {
        Long id = prepareEolImport("CR057-EOL-001");

        ReplayEventResult result = replayAppService.replay(
                id, ReplayVehicleImportEventCmd.builder().build(), "op-cr057", "CR057-Operator");

        // EOL_LIFECYCLE_ENSURE(SUCCESS)；无候选零件绑定/软件 → BINDING/SOFTWARE 无目标
        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getSuccessCount());

        String reachTime = jdbcTemplate.queryForObject(
                "SELECT reach_time FROM tb_veh_lifecycle WHERE vin = ? AND node = 'EOL'", String.class, VIN);
        assertNotNull(reachTime);
        // 1784391702000ms = 2026-07-19（UTC），应使用原批次 EOL_TIME 而非当前时刻
        assertTrue(reachTime.startsWith("2026-07-1"), "应使用原批次 EOL_TIME，实际=" + reachTime);
    }

    @Test
    @DisplayName("replayId 幂等：同 requestId 再次提交不重复执行")
    void replay_idempotentReplayId() {
        Long id = prepareTolImport("CR057-TOL-003", "CR057-TOLPN", "CR057-TOLSN", true, true, false);

        ReplayEventResult first = replayAppService.replay(id,
                ReplayVehicleImportEventCmd.builder().requestId("CR057-REQ-001").build(), "op-cr057", "op");
        ReplayEventResult second = replayAppService.replay(id,
                ReplayVehicleImportEventCmd.builder().requestId("CR057-REQ-001").build(), "op-cr057", "op");

        assertEquals(first.getReplayId(), second.getReplayId());
        Integer outboxCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_vmd_outbox WHERE source_type='IMPORT_EVENT_REPLAY' AND source_ref_id=?",
                Integer.class, first.getReplayId());
        assertEquals(2, outboxCount); // 不重复：仍只有绑定+软件各1
    }

    @Test
    @DisplayName("preview 预检返回导入类型/候选车辆数/可执行动作")
    void preview_returnsActions() {
        Long id = prepareTolImport("CR057-TOL-004", "CR057-TOLPN", "CR057-TOLSN", true, true, false);

        VehicleImportReplayPreview preview = replayAppService.preview(id);

        assertEquals("TOL", preview.getImportType());
        assertEquals("CR057-TOL-004", preview.getBatchNum());
        assertEquals(1, preview.getCandidateVinCount());
        assertEquals(3, preview.getActions().size());
        assertEquals("TOL_LIFECYCLE_ENSURE", preview.getActions().get(0).getActionType());
        assertTrue(preview.getActions().stream()
                .anyMatch(a -> "BINDING_EVENT_REPLAY".equals(a.getActionType()) && a.getEligibleCount() == 1));
        assertTrue(preview.getActions().stream()
                .anyMatch(a -> "SOFTWARE_INVENTORY_EVENT_REPLAY".equals(a.getActionType()) && a.getEligibleCount() == 1));
    }

    @Test
    @DisplayName("PRODUCE 补发兼容回归：仅生产事件入 Outbox，状态 QUEUED")
    void produceReplay_regression() {
        Long id = prepareProduceImport("CR057-PROD-001");

        ReplayEventResult result = replayAppService.replay(
                id, ReplayVehicleImportEventCmd.builder().build(), "op-cr057", "op");

        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getQueuedCount());
        assertEquals("QUEUED", jdbcTemplate.queryForObject(
                "SELECT status FROM tb_veh_import_event_replay WHERE replay_id = ?", String.class, result.getReplayId()));
        assertEquals(1, result.getActionResults().size());
        assertEquals("PRODUCE_EVENT", result.getActionResults().get(0).getActionType());

        String payload = jdbcTemplate.queryForObject(
                "SELECT payload FROM tb_vmd_outbox WHERE source_type='IMPORT_EVENT_REPLAY' AND source_ref_id=? "
                        + "AND event_type='VehicleProduceEvent'", String.class, result.getReplayId());
        assertTrue(payload.contains("\"eventType\":\"VehicleProduceEvent\""));
        assertTrue(payload.contains("\"replay\":true"));
    }

    @Test
    @DisplayName("绑定已失效 → 绑定事件不规划不发布，不产生历史脏快照")
    void tolReplay_bindingUnbound() {
        Long id = prepareTolImport("CR057-TOL-005", "CR057-TOLPN", "CR057-TOLSN", true, false, false);
        // 解绑
        jdbcTemplate.update("UPDATE tb_vehicle_part SET bind_state = 0, unbind_time = NOW() WHERE vin = ?", VIN);

        ReplayEventResult result = replayAppService.replay(
                id, ReplayVehicleImportEventCmd.builder().build(), "op-cr057", "op");

        // 无 active 绑定 → BINDING_EVENT_REPLAY 无规划目标（actionResults 不出现或 total=0）
        ReplayActionResult bindingResult = result.getActionResults().stream()
                .filter(a -> "BINDING_EVENT_REPLAY".equals(a.getActionType()))
                .findFirst().orElse(null);
        assertTrue(bindingResult == null || bindingResult.getTotalCount() == 0);
        // 不发布任何绑定事件
        Integer bindingOutbox = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_vmd_outbox WHERE source_type='IMPORT_EVENT_REPLAY' AND source_ref_id=? "
                        + "AND event_type='VehiclePartBindingChangedEvent'", Integer.class, result.getReplayId());
        assertEquals(0, bindingOutbox);
    }

    @Test
    @DisplayName("V57 迁移脚本存在且覆盖关键列与唯一约束")
    void v57MigrationScript_shouldExistAndCoverKeyColumns() throws IOException {
        Path migrationFile = Paths.get("src/main/resources/db/migration",
                "V57__Alter_veh_import_event_replay_cr057.sql");
        assertTrue(Files.exists(migrationFile), "V57 迁移脚本文件应该存在");
        String content = Files.readString(migrationFile);
        assertTrue(content.contains("import_type"), "应重命名 import_type");
        assertTrue(content.contains("requested_actions"), "应包含 requested_actions");
        assertTrue(content.contains("success_count"), "应包含 success_count");
        assertTrue(content.contains("skip_count"), "应包含 skip_count");
        assertTrue(content.contains("tb_veh_import_event_replay_item"), "应创建逐项审计表");
        assertTrue(content.contains("uk_replay_item"), "应包含逐项 UK(replay_id,action_type,aggregate_type,aggregate_id,aggregate_version)");
        assertTrue(content.contains("aggregate_version"), "应包含聚合版本列");
    }
}
