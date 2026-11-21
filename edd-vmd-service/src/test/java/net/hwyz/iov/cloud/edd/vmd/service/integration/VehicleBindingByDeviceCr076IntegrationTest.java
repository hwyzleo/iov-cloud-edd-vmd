package net.hwyz.iov.cloud.edd.vmd.service.integration;

import net.hwyz.iov.cloud.edd.vmd.api.vo.enums.VehicleBindingState;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VehiclePartBindingByDeviceExResponse;
import net.hwyz.iov.cloud.edd.vmd.api.vo.response.VehiclePartBindingExResponse;
import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.controller.service.ServiceVehiclePartBindingController;
import net.hwyz.iov.cloud.framework.web.exception.InputParamInvalidException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 设备→当前绑定车辆只读查询集成测试（VMD-DSN-CR-076，真实 MySQL 8 + Flyway V71）
 * <p>
 * 覆盖 TC-VMD-76-01～07：唯一绑定、节点类别回退、无 active 绑定、
 * 跨 VIN 冲突（不返回 VIN）、只读无副作用、参数非法、索引命中与既有契约回归。
 *
 * @author hwyz_leo
 * @since 2026-11-20
 */
class VehicleBindingByDeviceCr076IntegrationTest extends BaseTest {

    private static final String PREFIX = "CR076-";
    private static final String NODE_CODE = PREFIX + "NODE-IVI";
    private static final String DEVICE_CATEGORY = "IVI";

    private static final String VIN_BOUND = "CR076VINBOUND0001";
    private static final String VIN_NODE_FALLBACK = "CR076VINNODE00001";
    private static final String VIN_INACTIVE = "CR076VININACT0001";
    private static final String VIN_CONFLICT_A = "CR076VINCONFLA001";
    private static final String VIN_CONFLICT_B = "CR076VINCONFLB001";

    private static final String SN_BOUND = PREFIX + "SN-BOUND";
    private static final String SN_NODE_FALLBACK = PREFIX + "SN-NODE";
    private static final String SN_INACTIVE = PREFIX + "SN-INACTIVE";
    private static final String SN_CONFLICT = PREFIX + "SN-CONFLICT";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ServiceVehiclePartBindingController controller;

    @BeforeEach
    void setUpData() {
        cleanUp();
        jdbcTemplate.update("INSERT INTO tb_mdm_vehicle_node "
                        + "(code, name, node_type, device_category, func_domain, ota_support, sort, source, "
                        + "create_time, modify_time, row_version, row_valid) "
                        + "VALUES (?, ?, 'ECU', ?, 'BODY', 'NONE', 1, 'MANUAL', NOW(), NOW(), 1, 1)",
                NODE_CODE, PREFIX + "IVI节点", DEVICE_CATEGORY);

        // 快照类别命中（快照非空）
        Long boundPartId = insertPartInstance(PREFIX + "PN-001", SN_BOUND, null, DEVICE_CATEGORY);
        insertBinding(VIN_BOUND, boundPartId, 1);

        // 快照为空，回退按实例车载节点解析 MDM 节点类别
        Long nodeFallbackPartId = insertPartInstance(PREFIX + "PN-002", SN_NODE_FALLBACK, NODE_CODE, null);
        insertBinding(VIN_NODE_FALLBACK, nodeFallbackPartId, 1);

        // 仅有失效绑定记录
        Long inactivePartId = insertPartInstance(PREFIX + "PN-003", SN_INACTIVE, null, DEVICE_CATEGORY);
        insertBinding(VIN_INACTIVE, inactivePartId, 0);

        // 同一设备序列号跨两个 VIN 的 active 绑定（不同零件编码，绕过 uk_part_code_sn）
        Long conflictPartIdA = insertPartInstance(PREFIX + "PN-004", SN_CONFLICT, null, DEVICE_CATEGORY);
        Long conflictPartIdB = insertPartInstance(PREFIX + "PN-005", SN_CONFLICT, null, DEVICE_CATEGORY);
        insertBinding(VIN_CONFLICT_A, conflictPartIdA, 1);
        insertBinding(VIN_CONFLICT_B, conflictPartIdB, 1);
    }

    private void cleanUp() {
        jdbcTemplate.update("DELETE FROM tb_vehicle_part WHERE vin LIKE 'CR076%'");
        jdbcTemplate.update("DELETE FROM tb_part_info WHERE part_code LIKE 'CR076-%'");
        jdbcTemplate.update("DELETE FROM tb_mdm_vehicle_node WHERE code LIKE 'CR076-%'");
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

    private String indexColumn(String indexName, int seqInIndex) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME FROM information_schema.statistics "
                        + "WHERE table_schema = DATABASE() AND table_name = 'tb_part_info' "
                        + "AND index_name = ? AND seq_in_index = ?", indexName, seqInIndex);
        return rows.isEmpty() ? null : String.valueOf(rows.get(0).get("COLUMN_NAME"));
    }

    @Test
    @DisplayName("IT-076-01：Flyway V71 已应用——tb_part_info 存在 (sn, device_category) 组合索引")
    void shouldHaveCompositeIndex() {
        assertThat(indexColumn("idx_sn_device_category", 1)).isEqualTo("sn");
        assertThat(indexColumn("idx_sn_device_category", 2)).isEqualTo("device_category");
    }

    @Test
    @DisplayName("TC-VMD-76-01：唯一 active 绑定 → BOUND + VIN + factRef + updatedAt")
    void shouldReturnBoundForSingleActiveBinding() {
        VehiclePartBindingByDeviceExResponse response = controller.getVehicleByDevice(SN_BOUND, DEVICE_CATEGORY);

        assertThat(response.getBindingState()).isEqualTo(VehicleBindingState.BOUND);
        assertThat(response.getVin()).isEqualTo(VIN_BOUND);
        assertThat(response.getFactRef()).startsWith("vehiclePartBinding:" + VIN_BOUND + ":1:");
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("类别口径：实例快照为空时按 MDM 节点类别回退命中 → BOUND")
    void shouldResolveCategoryFromVehicleNodeWhenSnapshotNull() {
        VehiclePartBindingByDeviceExResponse response =
                controller.getVehicleByDevice(SN_NODE_FALLBACK, DEVICE_CATEGORY);

        assertThat(response.getBindingState()).isEqualTo(VehicleBindingState.BOUND);
        assertThat(response.getVin()).isEqualTo(VIN_NODE_FALLBACK);
    }

    @Test
    @DisplayName("TC-VMD-76-02：仅有失效绑定 → NO_ACTIVE_BINDING，无 VIN")
    void shouldReturnNoActiveBindingForInactiveOnly() {
        VehiclePartBindingByDeviceExResponse response = controller.getVehicleByDevice(SN_INACTIVE, DEVICE_CATEGORY);

        assertThat(response.getBindingState()).isEqualTo(VehicleBindingState.NO_ACTIVE_BINDING);
        assertThat(response.getVin()).isNull();
        assertThat(response.getFactRef()).isNull();
        assertThat(response.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("TC-VMD-76-02：类别不匹配 → NO_ACTIVE_BINDING（不误命中其他类别）")
    void shouldReturnNoActiveBindingForMismatchedCategory() {
        VehiclePartBindingByDeviceExResponse response =
                controller.getVehicleByDevice(SN_BOUND, "TBOX");

        assertThat(response.getBindingState()).isEqualTo(VehicleBindingState.NO_ACTIVE_BINDING);
        assertThat(response.getVin()).isNull();
    }

    @Test
    @DisplayName("TC-VMD-76-03：同设备多 VIN active → BINDING_CONFLICT，且不返回任何 VIN")
    void shouldReturnConflictWithoutVin() {
        VehiclePartBindingByDeviceExResponse response = controller.getVehicleByDevice(SN_CONFLICT, DEVICE_CATEGORY);

        assertThat(response.getBindingState()).isEqualTo(VehicleBindingState.BINDING_CONFLICT);
        assertThat(response.getVin()).isNull();
        assertThat(response.getFactRef()).isNull();
        assertThat(response.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("TC-VMD-76-04：查询只读——绑定事实与生命周期无变化、重复查询结果稳定")
    void shouldBeReadOnlyAndStable() {
        Map<String, Object> before = jdbcTemplate.queryForMap(
                "SELECT COUNT(*) AS cnt, MAX(modify_time) AS max_modify, MAX(row_version) AS max_version "
                        + "FROM tb_vehicle_part WHERE vin LIKE 'CR076%'");
        int lifecycleBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_veh_lifecycle WHERE vin LIKE 'CR076%'", Integer.class);

        VehiclePartBindingByDeviceExResponse first = controller.getVehicleByDevice(SN_BOUND, DEVICE_CATEGORY);
        VehiclePartBindingByDeviceExResponse second = controller.getVehicleByDevice(SN_BOUND, DEVICE_CATEGORY);

        Map<String, Object> after = jdbcTemplate.queryForMap(
                "SELECT COUNT(*) AS cnt, MAX(modify_time) AS max_modify, MAX(row_version) AS max_version "
                        + "FROM tb_vehicle_part WHERE vin LIKE 'CR076%'");
        int lifecycleAfter = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_veh_lifecycle WHERE vin LIKE 'CR076%'", Integer.class);

        assertThat(after).isEqualTo(before);
        assertThat(lifecycleAfter).isEqualTo(lifecycleBefore);
        assertThat(second.getBindingState()).isEqualTo(first.getBindingState());
        assertThat(second.getVin()).isEqualTo(first.getVin());
        assertThat(second.getFactRef()).isEqualTo(first.getFactRef());
        assertThat(second.getUpdatedAt()).isEqualTo(first.getUpdatedAt());
    }

    @Test
    @DisplayName("TC-VMD-76-05：参数非法 → 框架参数错误（100001），不返回 NO_ACTIVE_BINDING")
    void shouldRejectInvalidParams() {
        InputParamInvalidException exception = assertThrows(InputParamInvalidException.class,
                () -> controller.getVehicleByDevice(" ", DEVICE_CATEGORY));
        assertThat(exception.getCode()).isEqualTo(100001);
        assertThrows(InputParamInvalidException.class,
                () -> controller.getVehicleByDevice(SN_BOUND, ""));
    }

    @Test
    @DisplayName("TC-VMD-76-06：索引命中——按 sn + device_category 点查走 idx_sn_device_category")
    void shouldHitCompositeIndex() {
        List<Map<String, Object>> plan = jdbcTemplate.queryForList(
                "EXPLAIN SELECT id FROM tb_part_info WHERE row_valid = 1 AND sn = ? AND device_category = ?",
                SN_BOUND, DEVICE_CATEGORY);

        assertThat(plan).isNotEmpty();
        assertThat(String.valueOf(plan.get(0).get("key"))).isEqualTo("idx_sn_device_category");
        assertThat(String.valueOf(plan.get(0).get("type"))).isNotEqualTo("ALL");
    }

    @Test
    @DisplayName("TC-VMD-76-07：既有按 VIN active 与 bootstrap 契约不回归")
    void shouldKeepExistingBindingContracts() {
        List<VehiclePartBindingExResponse> active = controller.listActiveBindingsByVin(VIN_BOUND);
        assertThat(active).hasSize(1);
        assertThat(active.get(0).getVin()).isEqualTo(VIN_BOUND);

        List<VehiclePartBindingExResponse> bootstrap = controller.listBindingsForBootstrap(null, 100);
        assertThat(bootstrap).isNotEmpty();
        assertThat(bootstrap).anyMatch(item -> VIN_BOUND.equals(item.getVin()));

        // 失效绑定仍不出现在 active 查询中
        assertThat(controller.listActiveBindingsByVin(VIN_INACTIVE)).isEmpty();
    }

}
