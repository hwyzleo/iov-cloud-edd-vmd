package net.hwyz.iov.cloud.edd.vmd.service.integration;

import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartInboundAppService;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.InboundSourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartInfoChangeLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * 属性历史 INSERT 失败整体回滚集成测试（VMD-DSN-CR-072 IT-072-03，真实 MySQL）
 * <p>
 * @MockBean 替换 change_log 仓库注入审计失败：patch 必须整体回滚——实例 row_version/CAS、类别、
 * 属性历史均无半更新。mock 单测不能证明事务回滚，故在真实 MySQL 事务中验证。
 *
 * @author hwyz_leo
 * @since 2026-10-30
 */
class DeviceCategoryRollbackCr072IntegrationTest extends BaseTest {

    private static final String PREFIX = "CR072R-";
    private static final String PART_CODE = PREFIX + "PN001";
    private static final String SN = PREFIX + "SN001";
    private static final String NODE_CODE = PREFIX + "DCU1";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PartInboundAppService partInboundAppService;

    @MockBean
    private PartInfoChangeLogRepository failingChangeLogRepository;

    @BeforeEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM tb_part_info_change_log WHERE part_id IN "
                + "(SELECT id FROM tb_part_info WHERE part_code LIKE 'CR072R-%')");
        jdbcTemplate.update("DELETE FROM tb_part_info WHERE part_code LIKE 'CR072R-%'");
        jdbcTemplate.update("DELETE FROM tb_mdm_vehicle_node WHERE code LIKE 'CR072R-%'");
        jdbcTemplate.update("DELETE FROM tb_mdm_part WHERE code LIKE 'CR072R-%'");
    }

    @Test
    @DisplayName("IT-072-03：属性历史 INSERT 失败 → 实例与 CAS 整体回滚，无半更新")
    void historyInsertFailure_rollsBackWholeMutation() {
        jdbcTemplate.update("INSERT INTO tb_mdm_part (code, name, part_type, status, is_software, "
                + "is_accurately_traced, fota_upgradeable, source, create_time, modify_time, row_valid) "
                + "VALUES (?, 'CR072R Part', 'OTHER', 'ACTIVE', 0, 0, 0, 'MDM', NOW(), NOW(), 1)", PART_CODE);
        jdbcTemplate.update("INSERT INTO tb_mdm_vehicle_node "
                + "(code, name, device_category, source, create_time, modify_time, row_valid) "
                + "VALUES (?, 'CR072R Node', 'DCU', 'MDM', NOW(), NOW(), 1)", NODE_CODE);
        jdbcTemplate.update("INSERT INTO tb_part_info (part_code, sn, vehicle_node_code, source, instance_state, "
                + "create_time, modify_time, row_valid) VALUES (?, ?, ?, 'MANUAL', 0, NOW(), NOW(), 1)",
                PART_CODE, SN, NODE_CODE);
        Long partId = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_part_info WHERE part_code = ? AND sn = ?", Long.class, PART_CODE, SN);

        doThrow(new RuntimeException("审计写入失败（测试注入）")).when(failingChangeLogRepository).insert(any());

        PartInboundAppService.PartInboundResult result = partInboundAppService.processInbound(
                List.of(PartInboundAppService.PartInboundRecord.builder()
                        .partCode(PART_CODE).sn(SN).vehicleNodeCode(NODE_CODE)
                        .batchNum(PREFIX + "B001").inboundBatchNo(PREFIX + "B001")
                        .build()),
                InboundSourceType.MANUAL, null);

        assertThat(result.getSuccessCount()).isZero();
        assertThat(result.getFailureCount()).isEqualTo(1);
        // 整体回滚：类别未落、row_version 不变、无属性历史
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT device_category, row_version FROM tb_part_info WHERE id = ?", partId);
        assertThat(row.get("device_category")).isNull();
        assertThat(((Number) row.get("row_version")).intValue()).isEqualTo(1);
        Integer logCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_part_info_change_log WHERE part_id = ?", Integer.class, partId);
        assertThat(logCount).isZero();
    }

}
