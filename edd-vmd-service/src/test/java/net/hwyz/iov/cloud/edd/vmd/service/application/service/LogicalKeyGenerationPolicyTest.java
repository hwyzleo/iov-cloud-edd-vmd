package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.DeviceBusinessKey;
import net.hwyz.iov.cloud.edd.vmd.service.support.KeyprovEnvelopeTestFixtures;
import net.hwyz.iov.cloud.framework.security.crypto.model.KeyPurpose;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LogicalKeyGenerationPolicy 单元测试（VMD-DSN-CR-066 §5/§7.2）。
 *
 * @author hwyz_leo
 * @since 2026-10-22
 */
class LogicalKeyGenerationPolicyTest {

    private final LogicalKeyGenerationPolicy policy = new LogicalKeyGenerationPolicy();

    private DeviceBusinessKey row(Long rootVersion, String derivationVersion, String materialModel,
                                  String deviceSn, String domain, String purpose) {
        return DeviceBusinessKey.builder()
                .deviceSn(deviceSn)
                .businessDomain(domain)
                .purpose(purpose)
                .materialModel(materialModel)
                .rootKeyVersion(rootVersion)
                .derivationVersion(derivationVersion)
                .build();
    }

    @Test
    void sameLogicalGeneration_true_whenGenerationIdentical() {
        DeviceBusinessKey a = row(7L, "v1", "DERIVED_ROOT", "SN-1", "V2C_COMM", "UPLINK_AEAD");
        DeviceBusinessKey b = row(7L, "v1", "DERIVED_ROOT", "SN-1", "V2C_COMM", "UPLINK_AEAD");
        assertTrue(policy.sameLogicalGeneration(a, b));
    }

    @Test
    void sameLogicalGeneration_false_onAnyGenerationDimensionChange() {
        DeviceBusinessKey base = row(7L, "v1", "DERIVED_ROOT", "SN-1", "V2C_COMM", "UPLINK_AEAD");
        // rootKeyVersion 变化
        assertFalse(policy.sameLogicalGeneration(base, row(8L, "v1", "DERIVED_ROOT", "SN-1", "V2C_COMM", "UPLINK_AEAD")));
        // derivationVersion 变化
        assertFalse(policy.sameLogicalGeneration(base, row(7L, "v2", "DERIVED_ROOT", "SN-1", "V2C_COMM", "UPLINK_AEAD")));
        // materialModel 变化
        assertFalse(policy.sameLogicalGeneration(base, row(7L, "v1", "RANDOM_MATERIAL", "SN-1", "V2C_COMM", "UPLINK_AEAD")));
        // purpose 变化
        assertFalse(policy.sameLogicalGeneration(base, row(7L, "v1", "DERIVED_ROOT", "SN-1", "V2C_COMM", "DOWNLINK_AEAD")));
        // 设备/锚上下文变化
        assertFalse(policy.sameLogicalGeneration(base, row(7L, "v1", "DERIVED_ROOT", "SN-2", "V2C_COMM", "UPLINK_AEAD")));
        assertFalse(policy.sameLogicalGeneration(base, row(7L, "v1", "DERIVED_ROOT", "SN-1", "OTHER_DOMAIN", "UPLINK_AEAD")));
    }

    @Test
    void matchesResultGeneration_checksRootAndDerivationAndKeyId() {
        DeviceBusinessKey row = DeviceBusinessKey.builder()
                .keyId("KEY-1").materialModel("DERIVED_ROOT").rootKeyVersion(7L).derivationVersion("v1").build();
        var sameResult = KeyprovEnvelopeTestFixtures.derivedWrappedKey("KEY-1", KeyPurpose.UPLINK_AEAD,
                3L, 7L, "v1", "OP-1");
        assertTrue(policy.matchesResultGeneration(row, sameResult));

        var diffRoot = KeyprovEnvelopeTestFixtures.derivedWrappedKey("KEY-2", KeyPurpose.UPLINK_AEAD,
                3L, 8L, "v1", "OP-2");
        assertFalse(policy.matchesResultGeneration(row, diffRoot), "rootKeyVersion 变化 → 非同一逻辑材料");
    }

    @Test
    void logicalIdentityDigest_deterministicAndSensitiveToInputs() {
        byte[] d1 = policy.logicalIdentityDigest("V2C_COMM_ROOT", "HWYZTEST000000001", "UPLINK_AEAD", 7L, "v1");
        assertNotNull(d1);
        assertEquals(32, d1.length, "BINARY(32) 摘要");

        // 确定性
        assertArrayEquals(d1, policy.logicalIdentityDigest("V2C_COMM_ROOT", "HWYZTEST000000001", "UPLINK_AEAD", 7L, "v1"));
        // 任一输入变化 → 摘要变化
        assertFalse(java.util.Arrays.equals(d1,
                policy.logicalIdentityDigest("V2C_COMM_ROOT", "HWYZTEST000000001", "UPLINK_AEAD", 8L, "v1")), "root 版本变化应改变摘要");
        assertFalse(java.util.Arrays.equals(d1,
                policy.logicalIdentityDigest("V2C_COMM_ROOT", "HWYZTEST000000001", "DOWNLINK_AEAD", 7L, "v1")), "purpose 变化应改变摘要");
        assertFalse(java.util.Arrays.equals(d1,
                policy.logicalIdentityDigest("V2C_COMM_ROOT", "HWYZTEST000000002", "UPLINK_AEAD", 7L, "v1")), "锚变化应改变摘要");
    }

    @Test
    void logicalIdentityDigest_nullWhenRootVersionUnknown() {
        // CR-066 §7.2：根版本 Provider 返回前无法确定时不得猜测最终摘要
        assertNull(policy.logicalIdentityDigest("V2C_COMM_ROOT", "HWYZTEST000000001", "UPLINK_AEAD", null, "v1"));
    }
}
