package net.hwyz.iov.cloud.edd.vmd.service.integration;

import cn.hutool.crypto.digest.DigestUtil;
import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.ImportOperationAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartSoftwareSnapshotProjectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VMD-DSN-CR-071 集成测试
 * <p>
 * 覆盖（§12.2 核心场景）：统一导入操作门面（REIMPORT/REPLAY）、
 * EOL 原批次定向重导补全 software_ver（SSOT 幂等命中 → 投影刷新）、
 * 属性历史同事务、REPLAY 零主体写入、同 operationId 幂等重试。
 * 使用真实 MySQL 8 + Flyway 全链路（V66→V67/V68 新表）。
 *
 * @author CR-071
 */
class ImportOperationCr071IntegrationTest extends BaseTest {

    private static final String BATCH_NUM = "CR071-EOL-001";
    private static final String VIN = "CR071-VIN001";
    private static final String PART_CODE = "CR071-PN001";
    private static final String SN = "CR071-SN001";
    private static final String SW_PART_NO = "CR071-SW001";
    private static final String SW_VERSION = "V1.0.0";
    private static final String SOURCE_EVENT_ID =
            DigestUtil.md5Hex(BATCH_NUM + "_" + VIN + "_" + SN + "_" + SW_PART_NO);

    @Autowired
    private ImportOperationAppService importOperationAppService;

    @Autowired
    private PartSoftwareSnapshotProjectionService softwareSnapshotProjectionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM tb_part_info_change_log WHERE part_id IN "
                + "(SELECT id FROM tb_part_info WHERE part_code LIKE 'CR071-%')");
        jdbcTemplate.update("DELETE FROM tb_vehicle_info_change_log WHERE vin LIKE 'CR071-%'");
        jdbcTemplate.update("DELETE FROM tb_import_operation_item WHERE operation_id IN "
                + "(SELECT operation_id FROM tb_import_operation WHERE batch_num LIKE 'CR071-%')");
        jdbcTemplate.update("DELETE FROM tb_import_operation WHERE batch_num LIKE 'CR071-%'");
        jdbcTemplate.update("DELETE FROM tb_vmd_outbox WHERE aggregate_id LIKE 'CR071-%' OR message_key LIKE 'CR071-%'");
        jdbcTemplate.update("DELETE FROM tb_part_software_installation WHERE part_id IN "
                + "(SELECT id FROM tb_part_info WHERE part_code LIKE 'CR071-%')");
        jdbcTemplate.update("DELETE FROM tb_vehicle_part WHERE vin LIKE 'CR071-%'");
        jdbcTemplate.update("DELETE FROM tb_part_info WHERE part_code LIKE 'CR071-%'");
        jdbcTemplate.update("DELETE FROM tb_veh_basic_info WHERE vin LIKE 'CR071-%'");
        jdbcTemplate.update("DELETE FROM tb_veh_import_data WHERE batch_num LIKE 'CR071-%'");
    }

    /**
     * 预置：原批次 EOL 导入（handle=true）+ 现有零件（software_ver 空，模拟线上缺口）
     * + 已绑定 + SSOT 软件实装记录（EOL 已写 SOFTWARE_TYPE=APP）
     */
    private Long seedEolState() {
        String dataJson = "{\"REQUEST\":{\"DATA\":{\"ITEMS\":[{\"VIN\":\"" + VIN + "\",\"EOL_RESULT\":\"PASS\","
                + "\"ECU_BASELINE\":[{\"SN\":\"" + SN + "\",\"ASSEMBLY_PART_NO\":\"" + PART_CODE + "\","
                + "\"VEHICLE_NODE\":\"TBOX_5G\",\"HARDWARE_PART_NO\":\"HW1\",\"HARDWARE_VERSION\":\"H1\","
                + "\"SOFTWARE\":[{\"SOFTWARE_TYPE\":\"APP\",\"SOFTWARE_PART_NO\":\"" + SW_PART_NO + "\","
                + "\"SOFTWARE_VERSION\":\"" + SW_VERSION + "\"}]}]}]}}}";
        jdbcTemplate.update("INSERT INTO tb_veh_import_data (batch_num, type, version, data, handle, create_time, modify_time) "
                + "VALUES (?, 'EOL', '1.0', ?, 1, NOW(), NOW())", BATCH_NUM, dataJson);
        Long importDataId = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_veh_import_data WHERE batch_num = ?", Long.class, BATCH_NUM);

        // 车辆（eol_result 空，等待重导补缺）
        jdbcTemplate.update("INSERT INTO tb_veh_basic_info (vin, plant_code, brand_code, platform_code, "
                + "car_line_code, model_code, variant_code, configuration_code, create_time, modify_time, row_valid) "
                + "VALUES (?, 'M', 'B', 'P', 'S', 'M', 'V', 'C', NOW(), NOW(), 1)", VIN);

        // 零件实例（software_ver/part_type 空，source=MANUAL 模拟线上缺口）
        jdbcTemplate.update("INSERT INTO tb_part_info (part_code, sn, vehicle_node_code, source, "
                + "instance_state, software_ver, software_pn, create_time, modify_time, row_valid) "
                + "VALUES (?, ?, 'TBOX_5G', 'MANUAL', 1, NULL, NULL, NOW(), NOW(), 1)", PART_CODE, SN);
        Long partId = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_part_info WHERE part_code = ? AND sn = ?", Long.class, PART_CODE, SN);

        // 已绑定
        jdbcTemplate.update("INSERT INTO tb_vehicle_part (vin, part_id, vehicle_node_code, bind_state, create_time, modify_time, row_valid) "
                + "VALUES (?, ?, 'TBOX_5G', 1, NOW(), NOW(), 1)", VIN, partId);
        Long bindingId = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_vehicle_part WHERE vin = ? AND vehicle_node_code = 'TBOX_5G'",
                Long.class, VIN);

        // SSOT 软件实装（EOL 已写入，含 SOFTWARE_TYPE）
        jdbcTemplate.update("INSERT INTO tb_part_software_installation "
                + "(part_id, binding_id, vin_snapshot, software_target_code, software_part_no, software_version, "
                + "slot, install_state, change_type, source, source_event_id, reported_at, is_confirmed, "
                + "is_active_slot, inventory_version, software_type, create_time, modify_time, row_valid) "
                + "VALUES (?, ?, ?, ?, ?, ?, NULL, 'ACTIVE', 'INITIAL', 'EOL', ?, NOW(), 1, 1, 1, 'APP', NOW(), NOW(), 1)",
                partId, bindingId, VIN, SW_PART_NO, SW_PART_NO, SW_VERSION, SOURCE_EVENT_ID);

        return importDataId;
    }

    @Test
    @DisplayName("EOL 原批次重导：补全 software_ver/软件投影 + 属性历史 + 车辆 eol_result")
    void reimport_fillsSoftwareSnapshotAndAudits() {
        Long importDataId = seedEolState();
        Long partId = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_part_info WHERE part_code = ? AND sn = ?", Long.class, PART_CODE, SN);

        // When：REIMPORT 定向重导（原批次 VIN 子集）
        ImportOperationAppService.ExecuteResult result = importOperationAppService.execute(
                ImportOperationAppService.ExecuteRequest.builder()
                        .domain("VEHICLE").importDataId(importDataId).mode("REIMPORT")
                        .scope(List.of(VIN)).repairPolicy("FILL_MISSING")
                        .reason("集成测试重导").operatorId("tester").build());

        // Then：操作收敛
        assertThat(result.getStatus()).isEqualTo("SUCCEEDED");
        assertThat(result.getOperationId()).isNotBlank();
        Long operationId = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_import_operation WHERE operation_id = ?", Long.class, result.getOperationId());
        assertThat(operationId).isEqualTo(1L);
        Integer itemCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_import_operation_item WHERE operation_id = ?",
                Integer.class, result.getOperationId());
        assertThat(itemCount).isGreaterThanOrEqualTo(1);

        // 零件软件投影补全（applyManifest 幂等命中 → refreshFromCurrentSsot → CURRENT）
        Map<String, Object> part = jdbcTemplate.queryForMap(
                "SELECT software_ver, software_pn, software_snapshot_state, software_snapshot_target "
                        + "FROM tb_part_info WHERE id = ?", partId);
        assertThat(part.get("software_ver")).isEqualTo(SW_VERSION);
        assertThat(part.get("software_pn")).isEqualTo(SW_PART_NO);
        assertThat(part.get("software_snapshot_state")).isEqualTo("CURRENT");
        assertThat(part.get("software_snapshot_target")).isEqualTo("APP");

        // 属性历史（BIND 补列 / SOFTWARE_SNAPSHOT）
        Integer changeLogCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_part_info_change_log WHERE part_id = ?", Integer.class, partId);
        assertThat(changeLogCount).isGreaterThanOrEqualTo(1);

        // 车辆 eol_result 补缺（FILL_MISSING）
        String eolResult = jdbcTemplate.queryForObject(
                "SELECT eol_result FROM tb_veh_basic_info WHERE vin = ?", String.class, VIN);
        assertThat(eolResult).isEqualTo("PASS");

        // SSOT 未重复写入（幂等命中，仍 1 条）
        Integer ssotCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_part_software_installation WHERE part_id = ?", Integer.class, partId);
        assertThat(ssotCount).isEqualTo(1);
    }

    @Test
    @DisplayName("REPLAY 仅补发事件，不写主体（software_ver 保持空）")
    void replay_doesNotWriteSubjects() {
        Long importDataId = seedEolState();
        Long partId = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_part_info WHERE part_code = ? AND sn = ?", Long.class, PART_CODE, SN);

        ImportOperationAppService.ExecuteResult result = importOperationAppService.execute(
                ImportOperationAppService.ExecuteRequest.builder()
                        .domain("VEHICLE").importDataId(importDataId).mode("REPLAY")
                        .scope(List.of(VIN)).reason("集成测试补发").operatorId("tester").build());

        assertThat(result.getStatus()).isEqualTo("SUCCEEDED");

        // 主体零修改：software_ver 仍为空
        String softwareVer = jdbcTemplate.queryForObject(
                "SELECT software_ver FROM tb_part_info WHERE id = ?", String.class, partId);
        assertThat(softwareVer).isNull();
        // 事件经 Outbox 补发
        Integer outboxCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_vmd_outbox WHERE source_type = 'IMPORT_EVENT_REPLAY' "
                        + "AND message_key = ?", Integer.class, VIN);
        assertThat(outboxCount).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("软件投影从 SSOT 刷新：唯一 APP 候选 → CURRENT + 成对投影")
    void projection_refreshFromSsot_setsCurrent() {
        Long importDataId = seedEolState();
        Long partId = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_part_info WHERE part_code = ? AND sn = ?", Long.class, PART_CODE, SN);

        // 直接刷新（applyManifest 幂等命中的投影修复路径）
        PartSoftwareSnapshotProjectionService.ProjectionResult result =
                softwareSnapshotProjectionService.refreshFromCurrentSsot(partId,
                        PartSoftwareSnapshotProjectionService.ProjectionContext.builder()
                                .operationId("it-proj").operatorId("tester").source("EOL").build());

        assertThat(result.state()).isEqualTo("CURRENT");
        assertThat(result.outcome()).isEqualTo("APPLIED");

        Map<String, Object> part = jdbcTemplate.queryForMap(
                "SELECT software_ver, software_pn, software_snapshot_state FROM tb_part_info WHERE id = ?", partId);
        assertThat(part.get("software_ver")).isEqualTo(SW_VERSION);
        assertThat(part.get("software_pn")).isEqualTo(SW_PART_NO);
        assertThat(part.get("software_snapshot_state")).isEqualTo("CURRENT");

        // 属性历史（SOFTWARE_SNAPSHOT）
        Integer snapshotLogs = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_part_info_change_log WHERE part_id = ? AND change_type = 'SOFTWARE_SNAPSHOT'",
                Integer.class, partId);
        assertThat(snapshotLogs).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("同 operationId 同摘要重试 → 幂等返回原结果，不重复写入")
    void reimport_sameOperationId_isIdempotent() {
        Long importDataId = seedEolState();
        Long partId = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_part_info WHERE part_code = ? AND sn = ?", Long.class, PART_CODE, SN);
        String operationId = "cr071-op-idempotent";

        ImportOperationAppService.ExecuteRequest request = ImportOperationAppService.ExecuteRequest.builder()
                .domain("VEHICLE").importDataId(importDataId).mode("REIMPORT")
                .scope(List.of(VIN)).repairPolicy("FILL_MISSING")
                .reason("集成测试重导").operatorId("tester")
                .operationId(operationId).build();

        ImportOperationAppService.ExecuteResult first = importOperationAppService.execute(request);
        assertThat(first.getStatus()).isEqualTo("SUCCEEDED");

        // 同 operationId 同摘要重试 → 返回原结果（不重新执行）
        ImportOperationAppService.ExecuteResult second = importOperationAppService.execute(request);
        assertThat(second.getOperationId()).isEqualTo(operationId);
        assertThat(second.getStatus()).isEqualTo(first.getStatus());

        // 软件记录不重复
        Integer ssotCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_part_software_installation WHERE part_id = ?", Integer.class, partId);
        assertThat(ssotCount).isEqualTo(1);
    }

}
