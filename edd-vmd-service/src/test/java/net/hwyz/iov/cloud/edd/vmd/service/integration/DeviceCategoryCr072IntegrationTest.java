package net.hwyz.iov.cloud.edd.vmd.service.integration;

import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartInboundAppService;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartInfoMutationAppService;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.InboundSourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 实例 device_category 最小快照集成测试（VMD-DSN-CR-072，真实 MySQL 8 + Flyway V70）
 * <p>
 * 覆盖：Flyway 加列、入站补缺落库与审计 metadata、节点冲突无半更新、解析失败记录级失败、
 * CAS 并发无陈旧覆盖。历史 INSERT 失败整体回滚见 DeviceCategoryRollbackCr072IntegrationTest。
 *
 * @author hwyz_leo
 * @since 2026-10-30
 */
class DeviceCategoryCr072IntegrationTest extends BaseTest {

    private static final String PREFIX = "CR072-";
    private static final String PART_CODE = PREFIX + "PN001";
    private static final String SN = PREFIX + "SN001";
    private static final String NODE_CODE = PREFIX + "DCU1";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PartInboundAppService partInboundAppService;

    @Autowired
    private PartInfoMutationAppService partInfoMutationAppService;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM tb_part_info_change_log WHERE part_id IN "
                + "(SELECT id FROM tb_part_info WHERE part_code LIKE 'CR072-%')");
        jdbcTemplate.update("DELETE FROM tb_vehicle_part WHERE vin LIKE 'CR072-%' OR part_id IN "
                + "(SELECT id FROM tb_part_info WHERE part_code LIKE 'CR072-%')");
        jdbcTemplate.update("DELETE FROM tb_part_info WHERE part_code LIKE 'CR072-%'");
        jdbcTemplate.update("DELETE FROM tb_mdm_vehicle_node WHERE code LIKE 'CR072-%'");
        jdbcTemplate.update("DELETE FROM tb_mdm_part WHERE code LIKE 'CR072-%'");
        jdbcTemplate.update("DELETE FROM tb_part_import_data WHERE batch_num LIKE 'CR072-%'");
    }

    private void seedMdmPart(String code) {
        jdbcTemplate.update("INSERT INTO tb_mdm_part (code, name, part_type, status, is_software, "
                + "is_accurately_traced, fota_upgradeable, source, create_time, modify_time, row_valid) "
                + "VALUES (?, ?, 'OTHER', 'ACTIVE', 0, 0, 0, 'MDM', NOW(), NOW(), 1)", code);
    }

    private void seedMdmNode(String code, String category) {
        jdbcTemplate.update("INSERT INTO tb_mdm_vehicle_node "
                + "(code, name, device_category, source, create_time, modify_time, row_valid) "
                + "VALUES (?, ?, ?, 'MDM', NOW(), NOW(), 1)", code, category);
    }

    private Long seedPartInstance(String partCode, String sn, String nodeCode) {
        jdbcTemplate.update("INSERT INTO tb_part_info (part_code, sn, vehicle_node_code, source, instance_state, "
                + "create_time, modify_time, row_valid) VALUES (?, ?, ?, 'MANUAL', 0, NOW(), NOW(), 1)",
                partCode, sn, nodeCode);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM tb_part_info WHERE part_code = ? AND sn = ?", Long.class, partCode, sn);
    }

    @Test
    @DisplayName("IT-072-01：Flyway V70 已应用——tb_part_info 存在 device_category 列（NULL 可空、无索引）")
    void flywayV70_deviceCategoryColumnExists() {
        List<Map<String, Object>> cols = jdbcTemplate.queryForList(
                "SELECT column_name, is_nullable FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 'tb_part_info' AND column_name = 'device_category'");
        assertThat(cols).hasSize(1);
        assertThat(String.valueOf(cols.get(0).get("IS_NULLABLE"))).isEqualTo("YES");
        List<Map<String, Object>> idx = jdbcTemplate.queryForList(
                "SELECT index_name FROM information_schema.statistics "
                        + "WHERE table_schema = DATABASE() AND table_name = 'tb_part_info' AND column_name = 'device_category'");
        assertThat(idx).isEmpty(); // 首版不建索引
    }

    @Test
    @DisplayName("IT-072-02：入站补缺——device_category 同事务落库，审计 metadata 含服务器解析证据")
    void inbound_fillsDeviceCategory_withAuditMetadata() {
        seedMdmPart(PART_CODE);
        seedMdmNode(NODE_CODE, "DCU");
        Long partId = seedPartInstance(PART_CODE, SN, NODE_CODE);

        PartInboundAppService.PartInboundResult result = partInboundAppService.processInbound(
                List.of(PartInboundAppService.PartInboundRecord.builder()
                        .partCode(PART_CODE).sn(SN).vehicleNodeCode(NODE_CODE)
                        .batchNum(PREFIX + "B001").inboundBatchNo(PREFIX + "B001")
                        .build()),
                InboundSourceType.MANUAL, null);

        assertThat(result.getFailureCount()).isZero();
        assertThat(result.getInvalidCount()).isZero();
        String category = jdbcTemplate.queryForObject(
                "SELECT device_category FROM tb_part_info WHERE id = ?", String.class, partId);
        assertThat(category).isEqualTo("DCU");
        // 审计 metadata：服务器构造解析证据（节点/类别/来源标识），与实际变更同事务写入
        String meta = jdbcTemplate.queryForObject(
                "SELECT metadata_json FROM tb_part_info_change_log WHERE part_id = ? ORDER BY id DESC LIMIT 1",
                String.class, partId);
        assertThat(meta).contains("\"vehicleNodeCode\":\"" + NODE_CODE + "\"");
        assertThat(meta).contains("\"deviceCategory\":\"DCU\"");
    }

    @Test
    @DisplayName("IT-072-05：节点冲突（当前与输入均非空且不同）→ 拒绝且无部分写入（真实事务）")
    void nodeConflict_rejectsNoPartialWrite() {
        seedMdmPart(PART_CODE);
        seedMdmNode(NODE_CODE, "DCU");
        String otherNode = PREFIX + "DCU2";
        seedMdmNode(otherNode, "DCU");
        Long partId = seedPartInstance(PART_CODE, SN, NODE_CODE);

        PartInboundAppService.PartInboundResult result = partInboundAppService.processInbound(
                List.of(PartInboundAppService.PartInboundRecord.builder()
                        .partCode(PART_CODE).sn(SN).vehicleNodeCode(otherNode)
                        .batchNum(PREFIX + "B001").inboundBatchNo(PREFIX + "B001")
                        .build()),
                InboundSourceType.MANUAL, null);

        assertThat(result.getInvalidCount()).isEqualTo(1); // 节点冲突 → invalid，该项不提交
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT vehicle_node_code, device_category, row_version FROM tb_part_info WHERE id = ?", partId);
        assertThat(String.valueOf(row.get("vehicle_node_code"))).isEqualTo(NODE_CODE);
        assertThat(row.get("device_category")).isNull();
        assertThat(((Number) row.get("row_version")).intValue()).isEqualTo(1); // 无部分写入
    }

    @Test
    @DisplayName("IT-072-06：类别解析失败（节点投影缺失）→ 记录级失败不落库，批次其他项继续")
    void resolveFailure_recordLevelFailure_batchContinues() {
        String validCode = PART_CODE + "V2";
        seedMdmPart(PART_CODE);
        seedMdmPart(validCode);
        seedMdmNode(PREFIX + "DCU2", "DCU");
        Long missingNodePartId = seedPartInstance(PART_CODE, SN, PREFIX + "MISSING_NODE");
        seedPartInstance(validCode, PREFIX + "SN2", PREFIX + "DCU2");

        PartInboundAppService.PartInboundResult result = partInboundAppService.processInbound(
                List.of(
                        PartInboundAppService.PartInboundRecord.builder()
                                .partCode(PART_CODE).sn(SN).vehicleNodeCode(PREFIX + "MISSING_NODE")
                                .batchNum(PREFIX + "B001").inboundBatchNo(PREFIX + "B001").build(),
                        PartInboundAppService.PartInboundRecord.builder()
                                .partCode(validCode).sn(PREFIX + "SN2").vehicleNodeCode(PREFIX + "DCU2")
                                .batchNum(PREFIX + "B001").inboundBatchNo(PREFIX + "B001").build()),
                InboundSourceType.MANUAL, null);

        assertThat(result.getFailureCount()).isEqualTo(1);   // 投影未就绪 → 可重试 failure
        assertThat(result.getSuccessCount()).isEqualTo(1);   // 其他项继续
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT device_category, row_version FROM tb_part_info WHERE id = ?", missingNodePartId);
        assertThat(row.get("device_category")).isNull();
        assertThat(((Number) row.get("row_version")).intValue()).isEqualTo(1); // 失败项不落库
    }

    @Test
    @DisplayName("IT-072-04：CAS 并发——两线程同预期版本补缺，一个 APPLIED 一个 CONFLICT，无陈旧覆盖")
    void casConcurrent_noStaleOverwrite() throws Exception {
        seedMdmNode(NODE_CODE, "DCU");
        Long partId = seedPartInstance(PART_CODE, SN, NODE_CODE);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        java.util.concurrent.Future<PartInfoMutationAppService.MutationResult> f1 = pool.submit(() -> {
            ready.countDown();
            go.await();
            return patchCategory(partId, 1);
        });
        java.util.concurrent.Future<PartInfoMutationAppService.MutationResult> f2 = pool.submit(() -> {
            ready.countDown();
            go.await();
            return patchCategory(partId, 1);
        });
        ready.await(5, TimeUnit.SECONDS);
        go.countDown();
        PartInfoMutationAppService.MutationResult r1 = f1.get(15, TimeUnit.SECONDS);
        PartInfoMutationAppService.MutationResult r2 = f2.get(15, TimeUnit.SECONDS);
        pool.shutdown();

        int applied = (r1.isApplied() ? 1 : 0) + (r2.isApplied() ? 1 : 0);
        int conflicted = ("CONFLICT".equals(r1.outcome()) ? 1 : 0) + ("CONFLICT".equals(r2.outcome()) ? 1 : 0);
        assertThat(applied).isEqualTo(1);
        assertThat(conflicted).isEqualTo(1);
        String category = jdbcTemplate.queryForObject(
                "SELECT device_category FROM tb_part_info WHERE id = ?", String.class, partId);
        assertThat(category).isEqualTo("DCU"); // 无陈旧覆盖
        Integer rowVersion = jdbcTemplate.queryForObject(
                "SELECT row_version FROM tb_part_info WHERE id = ?", Integer.class, partId);
        assertThat(rowVersion).isEqualTo(2);
    }

    private PartInfoMutationAppService.MutationResult patchCategory(Long partId, int expectedRowVersion) {
        return partInfoMutationAppService.patch(
                net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.PartInfoPatchCmd.builder()
                        .partId(partId)
                        .vehicleNodeCode(NODE_CODE)
                        .deriveDeviceCategory(true)
                        .expectedRowVersion(expectedRowVersion)
                        .repairPolicy(PartInfoMutationAppService.POLICY_FILL_MISSING)
                        .changeType("INBOUND").operationSource("IT-CR072")
                        .operatorId("tester").phase("BASIC").seq(1)
                        .build());
    }

}
