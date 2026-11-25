package net.hwyz.iov.cloud.edd.vmd.service.integration;

import net.hwyz.iov.cloud.edd.vmd.api.vo.enums.VehicleBindingState;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VehiclePartBindingByDeviceExResponse;
import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.service.ServiceVehiclePartBindingController;
import net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd.VehicleNodeProjectionCommand;
import net.hwyz.iov.cloud.edd.vmd.service.application.mapper.MdmVehicleNodeProjectionMapper;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartCategoryConsistencyAppService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 零件派生设备类别事实源与自动收敛集成测试（EDD-VMD-DSN-CR-078，真实 MySQL 8 + Flyway）
 * <p>
 * 覆盖 TC-VMD-78-01～10：
 * <ul>
 *   <li>授权查询按 MDM 节点类别判定（节点优先、快照兜底），陈旧快照不再阻断；</li>
 *   <li>节点类别变更（Kafka 事件与 Bootstrap 快照共用投影内核）后自动收敛派生类别；</li>
 *   <li>自动收敛写属性变更历史（source=SYSTEM / trigger=NODE_CHANGED|SCHEDULED）；</li>
 *   <li>类别置空不触发收敛、定时收敛幂等（已一致 → SKIPPED）、既有契约回归。</li>
 * </ul>
 *
 * @author hwyz_leo
 * @since 2026-11-25
 */
class PartCategoryConvergenceCr078IntegrationTest extends BaseTest {

    private static final String PREFIX = "CR078-";
    private static final String NODE_CODE = PREFIX + "NODE-COCKPIT";
    private static final String PART_CODE = PREFIX + "PN-001";
    private static final String SN = PREFIX + "SN-001";
    private static final String VIN = "CR078VIN00000001";
    private static final String VIN_FALLBACK = "CR078VINFALLBK001";
    private static final String SN_FALLBACK = PREFIX + "SN-002";
    private static final String PART_CODE_FALLBACK = PREFIX + "PN-002";
    /** 节点事实源类别 */
    private static final String CATEGORY_NODE = "IVI";
    /** 陈旧派生快照（线上故障现场：DCU） */
    private static final String CATEGORY_STALE = "DCU";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ServiceVehiclePartBindingController controller;

    @Autowired
    private MdmVehicleNodeProjectionMapper mdmVehicleNodeProjectionMapper;

    @Autowired
    private PartCategoryConsistencyAppService partCategoryConsistencyAppService;

    @BeforeEach
    void setUpData() {
        cleanUp();
        insertNode(NODE_CODE, CATEGORY_STALE, 1L);
        Long partId = insertPartInstance(PART_CODE, SN, NODE_CODE, CATEGORY_STALE);
        insertBinding(VIN, partId, 1);
    }

    private void cleanUp() {
        jdbcTemplate.update("DELETE FROM tb_part_info_change_log WHERE part_id IN "
                + "(SELECT id FROM tb_part_info WHERE part_code LIKE 'CR078-%')");
        jdbcTemplate.update("DELETE FROM tb_vehicle_part WHERE vin LIKE 'CR078%'");
        jdbcTemplate.update("DELETE FROM tb_part_info WHERE part_code LIKE 'CR078-%'");
        jdbcTemplate.update("DELETE FROM tb_mdm_vehicle_node WHERE code LIKE 'CR078-%'");
    }

    private void insertNode(String code, String deviceCategory, long externalVersion) {
        jdbcTemplate.update("INSERT INTO tb_mdm_vehicle_node "
                        + "(code, name, node_type, device_category, func_domain, ota_support, sort, source, "
                        + "external_version, create_time, modify_time, row_version, row_valid) "
                        + "VALUES (?, ?, 'ECU', ?, 'COCKPIT', 'NONE', 1, 'MDM', ?, NOW(), NOW(), 1, 1)",
                code, PREFIX + "座舱域控", deviceCategory, externalVersion);
    }

    private Long insertPartInstance(String partCode, String sn, String nodeCode, String deviceCategory) {
        jdbcTemplate.update("INSERT INTO tb_part_info "
                        + "(part_code, sn, vehicle_node_code, device_category, source, instance_state, "
                        + "create_time, modify_time, row_version, row_valid) "
                        + "VALUES (?, ?, ?, ?, 'MANUAL', 0, NOW(), NOW(), 1, 1)",
                partCode, sn, nodeCode, deviceCategory);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM tb_part_info WHERE part_code = ? AND sn = ?", Long.class, partCode, sn);
    }

    private void insertBinding(String vin, Long partId, int bindState) {
        jdbcTemplate.update("INSERT INTO tb_vehicle_part "
                        + "(vin, part_id, bind_state, bind_time, create_time, modify_time, row_version, row_valid) "
                        + "VALUES (?, ?, ?, NOW(), NOW(), NOW(), 1, 1)",
                vin, partId, bindState);
    }

    private VehicleNodeProjectionCommand nodeCommand(String deviceCategory, long version) {
        return VehicleNodeProjectionCommand.builder()
                .code(NODE_CODE)
                .name(PREFIX + "座舱域控")
                .deviceCategory(deviceCategory)
                .funcDomain("COCKPIT")
                .nodeType("ECU")
                .otaSupport("NONE")
                .hsmCapability("HSM_FULL")
                .core(true)
                .sort(1)
                .externalRefId(PREFIX + "mdm-node")
                .externalVersion(version)
                .build();
    }

    private String partCategory(String partCode, String sn) {
        return jdbcTemplate.queryForObject(
                "SELECT device_category FROM tb_part_info WHERE part_code = ? AND sn = ?",
                String.class, partCode, sn);
    }

    private Map<String, Object> latestChangeLog(String partCode, String sn) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT l.source, l.operation_source, l.operator_id, l.metadata_json "
                        + "FROM tb_part_info_change_log l JOIN tb_part_info p ON p.id = l.part_id "
                        + "WHERE p.part_code = ? AND p.sn = ? ORDER BY l.id DESC LIMIT 1", partCode, sn);
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Test
    @DisplayName("TC-VMD-78-01：节点类别变更为 IVI 后自动收敛，且按 IVI 查询返回 BOUND（陈旧快照 DCU 不再阻断）")
    void shouldConvergeOnNodeCategoryChangeAndQueryByNodeCategory() {
        // 收敛前：派生快照=DCU，节点=DCU（一致），按 IVI 查不到
        assertThat(controller.getVehicleByDevice(SN, CATEGORY_NODE).getBindingState())
                .isEqualTo(VehicleBindingState.NO_ACTIVE_BINDING);

        // MDM 节点类别改为 IVI（Kafka 事件路径，version 1 → 2）
        mdmVehicleNodeProjectionMapper.apply(nodeCommand(CATEGORY_NODE, 2L));

        // 自动收敛：派生快照收敛为 IVI，并留下 SYSTEM/NODE_CHANGED 审计
        assertThat(partCategory(PART_CODE, SN)).isEqualTo(CATEGORY_NODE);
        Map<String, Object> changeLog = latestChangeLog(PART_CODE, SN);
        assertThat(changeLog).isNotNull();
        assertThat(changeLog.get("source")).isEqualTo("SYSTEM");
        assertThat(changeLog.get("operation_source")).isEqualTo("SYSTEM");
        assertThat(changeLog.get("operator_id")).isEqualTo("SYSTEM");
        assertThat(String.valueOf(changeLog.get("metadata_json"))).contains("NODE_CHANGED");
        assertThat(String.valueOf(changeLog.get("metadata_json"))).contains(NODE_CODE);

        // 授权查询：按节点类别命中，按陈旧快照值不再误命中
        VehiclePartBindingByDeviceExResponse bound = controller.getVehicleByDevice(SN, CATEGORY_NODE);
        assertThat(bound.getBindingState()).isEqualTo(VehicleBindingState.BOUND);
        assertThat(bound.getVin()).isEqualTo(VIN);
        assertThat(controller.getVehicleByDevice(SN, CATEGORY_STALE).getBindingState())
                .isEqualTo(VehicleBindingState.NO_ACTIVE_BINDING);
    }

    @Test
    @DisplayName("TC-VMD-78-01：节点类别与快照不一致时，按节点类别仍可命中（不等巡检/收敛）")
    void shouldJudgeByNodeCategoryEvenWhenSnapshotStale() {
        // 直接改节点类别（模拟 MDM 已改、零件快照尚未收敛）
        jdbcTemplate.update("UPDATE tb_mdm_vehicle_node SET device_category = ?, external_version = 2 "
                + "WHERE code = ?", CATEGORY_NODE, NODE_CODE);
        assertThat(partCategory(PART_CODE, SN)).isEqualTo(CATEGORY_STALE);

        VehiclePartBindingByDeviceExResponse response = controller.getVehicleByDevice(SN, CATEGORY_NODE);

        assertThat(response.getBindingState()).isEqualTo(VehicleBindingState.BOUND);
        assertThat(response.getVin()).isEqualTo(VIN);
    }

    @Test
    @DisplayName("TC-VMD-78-02：节点缺失时按派生快照兜底（兼容历史无节点数据）")
    void shouldFallbackToSnapshotWhenNodeAbsent() {
        Long partId = insertPartInstance(PART_CODE_FALLBACK, SN_FALLBACK, "CR078-NODE-ABSENT", CATEGORY_NODE);
        insertBinding(VIN_FALLBACK, partId, 1);

        VehiclePartBindingByDeviceExResponse response =
                controller.getVehicleByDevice(SN_FALLBACK, CATEGORY_NODE);

        assertThat(response.getBindingState()).isEqualTo(VehicleBindingState.BOUND);
        assertThat(response.getVin()).isEqualTo(VIN_FALLBACK);
    }

    @Test
    @DisplayName("TC-VMD-78-05：Bootstrap 快照路径（同一投影内核）同样触发自动收敛")
    void shouldConvergeFromBootstrapSnapshotPath() {
        mdmVehicleNodeProjectionMapper.apply(nodeCommand(CATEGORY_NODE, 2L));

        assertThat(partCategory(PART_CODE, SN)).isEqualTo(CATEGORY_NODE);
        assertThat(controller.getVehicleByDevice(SN, CATEGORY_NODE).getBindingState())
                .isEqualTo(VehicleBindingState.BOUND);
    }

    @Test
    @DisplayName("TC-VMD-78-07：节点类别被置空时不触发收敛（无有效期望值），快照保持原值")
    void shouldNotConvergeWhenNodeCategoryCleared() {
        mdmVehicleNodeProjectionMapper.apply(nodeCommand(null, 2L));

        assertThat(partCategory(PART_CODE, SN)).isEqualTo(CATEGORY_STALE);
    }

    @Test
    @DisplayName("TC-VMD-78-08：定时收敛入口按 SCHEDULED 记录审计，重复执行幂等（SKIPPED）")
    void shouldConvergeWithScheduledTriggerAndBeIdempotent() {
        jdbcTemplate.update("UPDATE tb_mdm_vehicle_node SET device_category = ?, external_version = 2 "
                + "WHERE code = ?", CATEGORY_NODE, NODE_CODE);
        int first = partCategoryConsistencyAppService
                .convergeByVehicleNodeCode(NODE_CODE, PartCategoryConsistencyAppService.TRIGGER_SCHEDULED)
                .getApplied();
        assertThat(first).isEqualTo(1);
        assertThat(partCategory(PART_CODE, SN)).isEqualTo(CATEGORY_NODE);
        Map<String, Object> changeLog = latestChangeLog(PART_CODE, SN);
        assertThat(String.valueOf(changeLog.get("metadata_json")))
                .contains(PartCategoryConsistencyAppService.TRIGGER_SCHEDULED);

        // 幂等：再次执行时该节点已无不一致条目，命中 0 条、无重复写历史
        PartCategoryConsistencyAppService.RepairResult second = partCategoryConsistencyAppService
                .convergeByVehicleNodeCode(NODE_CODE, PartCategoryConsistencyAppService.TRIGGER_SCHEDULED);
        assertThat(second.getTotal()).isZero();
        assertThat(second.getApplied()).isZero();
        Integer logCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_part_info_change_log WHERE part_id = "
                        + "(SELECT id FROM tb_part_info WHERE part_code = ? AND sn = ?)",
                Integer.class, PART_CODE, SN);
        assertThat(logCount).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-VMD-78-09：巡检只读统计与真实不一致数据一致（收敛后归零）")
    void shouldReflectInconsistencyCountBeforeAndAfterConvergence() {
        jdbcTemplate.update("UPDATE tb_mdm_vehicle_node SET device_category = ?, external_version = 2 "
                + "WHERE code = ?", CATEGORY_NODE, NODE_CODE);
        long before = partCategoryConsistencyAppService.countInconsistent();
        assertThat(before).isGreaterThanOrEqualTo(1);
        assertThat(partCategoryConsistencyAppService.listInconsistentVehicleNodeCodes()).contains(NODE_CODE);

        partCategoryConsistencyAppService
                .convergeByVehicleNodeCode(NODE_CODE, PartCategoryConsistencyAppService.TRIGGER_SCHEDULED);

        assertThat(partCategoryConsistencyAppService.countInconsistent()).isLessThan(before);
        assertThat(partCategoryConsistencyAppService.listInconsistentVehicleNodeCodes())
                .doesNotContain(NODE_CODE);
    }

    @Test
    @DisplayName("TC-VMD-78-10：既有绑定契约回归——三态与按 VIN 查询不受 CR-078 影响")
    void shouldKeepExistingBindingContracts() {
        mdmVehicleNodeProjectionMapper.apply(nodeCommand(CATEGORY_NODE, 2L));

        var activeBindings = controller.listActiveBindingsByVin(VIN);
        assertThat(activeBindings).hasSize(1);
        assertThat(activeBindings.get(0).getSn()).isEqualTo(SN);
        assertThat(controller.listBindingsForBootstrap(null, 100)).isNotNull();
    }
}
