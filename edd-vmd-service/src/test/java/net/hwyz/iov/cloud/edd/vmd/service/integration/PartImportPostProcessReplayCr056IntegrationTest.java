package net.hwyz.iov.cloud.edd.vmd.service.integration;

import cn.hutool.json.JSONUtil;
import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.ReplayPartImportPostProcessCmd;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.PartImportPostProcessReplayDto;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.result.ReplayPostProcessResult;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartImportPostProcessReplayAppService;
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
 * VMD-DSN-CR-056 集成测试
 * <p>
 * 覆盖：零件导入后置处理人工重放完整链路——从历史导入批次识别候选实例、读取当前
 * part_info / active vehicle_part / MDM 投影，逐项执行事件入 Outbox、绑定事实事件、
 * 下游联动与安全常量补偿的适用性判定与 SKIPPED 落账；主任务与逐项动作审计；
 * 不修改 part_import_data.handle/description 与 part_info 主体；再次重放使用新 replayId。
 * </p>
 *
 * @author CR-056
 */
@Rollback
class PartImportPostProcessReplayCr056IntegrationTest extends BaseTest {

    private static final String PART_CODE = "CR056_PN";
    private static final String SN = "CR056-SN001";

    @Autowired
    private PartImportPostProcessReplayAppService replayAppService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 每次执行前清理历史残留（BaseTest 无测试事务，@Rollback 不生效，需显式清理）
     */
    @org.junit.jupiter.api.BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM tb_part_import_postprocess_replay_item WHERE replay_id IN "
                + "(SELECT replay_id FROM tb_part_import_postprocess_replay WHERE part_import_data_id IN "
                + "(SELECT id FROM tb_part_import_data WHERE batch_num LIKE 'CR056-%'))");
        jdbcTemplate.update("DELETE FROM tb_part_import_postprocess_replay WHERE part_import_data_id IN "
                + "(SELECT id FROM tb_part_import_data WHERE batch_num LIKE 'CR056-%')");
        jdbcTemplate.update("DELETE FROM tb_vmd_outbox WHERE source_type = 'PART_POST_PROCESS_REPLAY'");
        jdbcTemplate.update("DELETE FROM tb_part_info WHERE part_code = ?", PART_CODE);
        jdbcTemplate.update("DELETE FROM tb_part_import_data WHERE batch_num LIKE 'CR056-%'");
        jdbcTemplate.update("DELETE FROM tb_mdm_part WHERE code = ?", PART_CODE);
    }

    /**
     * 准备导入记录 + 当前 part_info + MDM Part 投影（无车载节点 → 下游联动 / 安全预置均 SKIPPED）
     */
    private Long prepareImportData(String batchNum) {
        // 原导入记录（handle=1，description 固定，用于断言重放不改写）
        String data = "{\"REQUEST\":{\"HEAD\":{\"ACCOUNT\":\"SUP001\"},\"DATA\":{\"ITEMS\":"
                + "[{\"SN\":\"" + SN + "\",\"ASSEMBLY_PART_NO\":\"" + PART_CODE + "\"}]}}}";
        jdbcTemplate.update(
                "INSERT INTO tb_part_import_data (batch_num, part_code, version, data, handle, description) "
                        + "VALUES (?, ?, '1.0', ?, 1, 'original-desc')",
                batchNum, PART_CODE, data);

        // MDM Part 投影（P0，status=ACTIVE，节点为空）
        jdbcTemplate.update(
                "INSERT INTO tb_mdm_part (code, name, part_type, is_software, status, source) "
                        + "VALUES (?, 'CR056测试零件', 'OTHER', 0, 'ACTIVE', 'MDM')",
                PART_CODE);

        // 当前 part_info（游离实例，无绑定，无车载节点）
        jdbcTemplate.update(
                "INSERT INTO tb_part_info (part_code, sn, vehicle_node_code, instance_state, source, part_type, "
                        + "inbound_batch_no, last_inbound_time) "
                        + "VALUES (?, ?, NULL, 0, 'MANUAL', 'OTHER', ?, NOW())",
                PART_CODE, SN, batchNum);

        return jdbcTemplate.queryForObject(
                "SELECT id FROM tb_part_import_data WHERE batch_num = ?", Long.class, batchNum);
    }

    @Test
    @DisplayName("完整链路：事件动作入 Outbox、绑定/联动/安全补偿 SKIPPED，主任务与逐项动作审计落库，主体零修改")
    void replayPostProcess_fullFlow() {
        Long id = prepareImportData("CR056-B001");

        ReplayPostProcessResult result = replayAppService.replay(
                id, ReplayPartImportPostProcessCmd.builder().reason("集成测试重放").build(),
                "op-cr056", "CR056-Operator");

        // 结果：1 候选 × 6 动作；事件入队 1，其余 SKIPPED
        assertEquals(1, result.getItemCount());
        assertEquals(6, result.getActionCount());
        assertEquals(1, result.getQueuedCount());
        assertEquals(0, result.getSuccessCount());
        assertEquals(5, result.getSkippedCount());
        assertEquals(0, result.getFailureCount());

        // 主任务落库且终态 SUCCESS
        PartImportPostProcessReplayDto replay = replayAppService.getReplayByReplayId(result.getReplayId());
        assertNotNull(replay);
        assertEquals("SUCCESS", replay.getStatus());
        assertEquals(1, replay.getTotalItemCount());
        assertEquals(6, replay.getTotalActionCount());
        assertEquals(1, replay.getQueuedCount());
        assertEquals(5, replay.getSkippedCount());
        assertEquals(6, replay.getItems().size());

        // 动作明细：PART_INBOUND_EVENT=QUEUED，BINDING_FACT_EVENT=SKIPPED(NO_ACTIVE_BINDING)，其余 SKIPPED
        Map<String, String> statusByAction = new java.util.HashMap<>();
        replay.getItems().forEach(i -> statusByAction.put(i.getActionType(), i.getStatus()));
        assertEquals("QUEUED", statusByAction.get("PART_INBOUND_EVENT"));
        assertEquals("SKIPPED", statusByAction.get("BINDING_FACT_EVENT"));
        assertEquals("SKIPPED", statusByAction.get("TSP_SYNC"));
        assertEquals("SKIPPED", statusByAction.get("OTA_SYNC"));
        assertEquals("SKIPPED", statusByAction.get("IDK_SYNC"));
        assertEquals("SKIPPED", statusByAction.get("SECURITY_PRESET"));

        // 绑定事实事件无 active 绑定 → SKIPPED 原因 NO_ACTIVE_BINDING
        replay.getItems().stream()
                .filter(i -> "BINDING_FACT_EVENT".equals(i.getActionType()))
                .findFirst()
                .ifPresent(i -> assertEquals("NO_ACTIVE_BINDING", i.getSkipReason()));

        // 事件动作写 Outbox（PartInboundEvent，sourceRefId=replayId）
        Integer outboxCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_vmd_outbox WHERE source_type = 'PART_POST_PROCESS_REPLAY' "
                        + "AND source_ref_id = ?", Integer.class, result.getReplayId());
        assertEquals(1, outboxCount);

        // 主体零修改：part_import_data.handle/description 不变
        Map<String, Object> importRow = jdbcTemplate.queryForMap(
                "SELECT handle, description FROM tb_part_import_data WHERE id = ?", id);
        assertEquals(1, importRow.get("handle"));
        assertEquals("original-desc", importRow.get("description"));
    }

    @Test
    @DisplayName("再次重放使用新 replayId，动作级幂等收敛（新 replay 独立审计）")
    void replayPostProcess_secondReplayNewReplayId() {
        Long id = prepareImportData("CR056-B002");

        ReplayPostProcessResult first = replayAppService.replay(
                id, ReplayPartImportPostProcessCmd.builder().build(), "op-cr056", "CR056-Operator");
        ReplayPostProcessResult second = replayAppService.replay(
                id, ReplayPartImportPostProcessCmd.builder().build(), "op-cr056", "CR056-Operator");

        assertNotNull(first.getReplayId());
        assertNotNull(second.getReplayId());
        assertNotEquals(first.getReplayId(), second.getReplayId());
        assertEquals(1, second.getQueuedCount());

        // 两个 replay 各自 6 条动作明细
        PartImportPostProcessReplayDto replay2 = replayAppService.getReplayByReplayId(second.getReplayId());
        assertEquals(6, replay2.getItems().size());
    }

    @Test
    @DisplayName("V56 迁移脚本存在且包含两表关键列与唯一约束")
    void v56MigrationScript_shouldExistAndCoverKeyColumns() throws IOException {
        Path migrationFile = Paths.get("src/main/resources/db/migration",
                "V56__Create_part_import_postprocess_replay_cr056.sql");
        assertTrue(Files.exists(migrationFile), "V56 迁移脚本文件应该存在");
        String content = Files.readString(migrationFile);
        assertTrue(content.contains("tb_part_import_postprocess_replay"), "应创建主任务表");
        assertTrue(content.contains("tb_part_import_postprocess_replay_item"), "应创建动作明细表");
        assertTrue(content.contains("uk_replay_id"), "主任务 replay_id 唯一约束");
        assertTrue(content.contains("uk_replay_item"), "动作明细 UK(replay_id,part_code,sn,action_type)");
        assertTrue(content.contains("replay_id"), "应包含 replay_id 列");
        assertTrue(content.contains("idempotency_key"), "应包含幂等键列");
        assertTrue(content.contains("attempt_count"), "应包含尝试次数列");
        assertTrue(content.contains("FAILED_RETRYABLE"), "应包含 FAILED_RETRYABLE 状态注释");
    }

    @Test
    @DisplayName("V56 迁移脚本注释完整（replay 语义 / 幂等 / 隔离）")
    void v56MigrationScript_shouldContainSemanticsComments() throws IOException {
        Path migrationFile = Paths.get("src/main/resources/db/migration",
                "V56__Create_part_import_postprocess_replay_cr056.sql");
        String content = Files.readString(migrationFile);
        assertTrue(content.contains("动作级幂等"), "应注明动作级幂等");
        assertTrue(content.contains("逐项动作审计"), "应注明逐项动作审计");
        assertTrue(content.contains("CR-056"), "应标注 CR-056");
    }
}
