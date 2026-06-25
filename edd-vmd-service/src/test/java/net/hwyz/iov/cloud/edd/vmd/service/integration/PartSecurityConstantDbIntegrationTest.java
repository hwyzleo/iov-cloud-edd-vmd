package net.hwyz.iov.cloud.edd.vmd.service.integration;

import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.PartSecurityConstant;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SecurityConstantState;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.PartSecurityConstantRepository;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.PartSecurityPresetAppService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 零件安全常量预置 - 真实数据库集成测试
 * <p>
 * 继承 BaseTest 连接 dev 库，使用 MockKmsHsmClient，@Rollback 自动回滚。
 * 验证 tb_part_security_constant 表的实际读写行为。
 *
 * @author hwyz_leo
 */
class PartSecurityConstantDbIntegrationTest extends BaseTest {

    @Autowired
    private PartSecurityPresetAppService partSecurityPresetAppService;

    @Autowired
    private PartSecurityConstantRepository partSecurityConstantRepository;

    @Test
    @DisplayName("预置安全常量应成功写入tb_part_security_constant表")
    @Transactional
    void preset_shouldWriteToDatabase() throws Exception {
        String partCode = "TBOX_DB_TEST_001";
        String sn = "SN_DB_TEST_001";
        String chipUid = "HSM_DB_TEST_001";
        String batchNum = "BATCH_DB_TEST_001";

        // 执行预置（使用 MockKmsHsmClient）
        partSecurityPresetAppService.preset(partCode, sn, chipUid, batchNum);

        // 从数据库查询验证
        PartSecurityConstant saved = partSecurityConstantRepository.selectByPartCodeAndSn(partCode, sn);

        assertNotNull(saved, "安全常量记录应已写入数据库");
        assertEquals(partCode, saved.getPartCode());
        assertEquals(sn, saved.getSn());
        assertEquals(chipUid, saved.getChipUid());
        assertEquals("ROOT", saved.getConstantType());
        assertEquals(SecurityConstantState.PRESET, saved.getPresetState());
        assertNotNull(saved.getKmsKeyRef());
        assertNotNull(saved.getGenTime());
        assertEquals(batchNum, saved.getBatchNum());
    }

    @Test
    @DisplayName("重复预置已存在的PRESET状态记录应跳过")
    @Transactional
    void preset_shouldSkipWhenAlreadyPreset() throws Exception {
        String partCode = "TBOX_DB_TEST_002";
        String sn = "SN_DB_TEST_002";
        String chipUid = "HSM_DB_TEST_002";
        String batchNum = "BATCH_DB_TEST_002";

        // 第一次预置
        partSecurityPresetAppService.preset(partCode, sn, chipUid, batchNum);
        PartSecurityConstant first = partSecurityConstantRepository.selectByPartCodeAndSn(partCode, sn);
        assertNotNull(first);
        assertEquals(SecurityConstantState.PRESET, first.getPresetState());

        // 第二次预置（应跳过）
        partSecurityPresetAppService.preset(partCode, sn, "DIFFERENT_CHIP", "BATCH_NEW");
        PartSecurityConstant second = partSecurityConstantRepository.selectByPartCodeAndSn(partCode, sn);

        // chipUid 不应被更新
        assertEquals(chipUid, second.getChipUid());
        assertEquals(batchNum, second.getBatchNum());
    }

    @Test
    @DisplayName("安全常量记录应包含所有必要字段")
    @Transactional
    void preset_shouldContainAllRequiredFields() throws Exception {
        String partCode = "TBOX_DB_TEST_003";
        String sn = "SN_DB_TEST_003";
        String chipUid = "HSM_DB_TEST_003";
        String batchNum = "BATCH_DB_TEST_003";

        partSecurityPresetAppService.preset(partCode, sn, chipUid, batchNum);

        PartSecurityConstant saved = partSecurityConstantRepository.selectByPartCodeAndSn(partCode, sn);

        assertNotNull(saved);
        assertNotNull(saved.getId(), "主键应自动生成");
        assertNotNull(saved.getCreateTime(), "创建时间应自动生成");
        assertNotNull(saved.getLastAttemptTime(), "最后尝试时间应被记录");
        assertNotNull(saved.getKmsProvider(), "KMS提供方应被记录");
        assertNotNull(saved.getAlgorithm());
        assertNotNull(saved.getKeySpec());
    }
}
