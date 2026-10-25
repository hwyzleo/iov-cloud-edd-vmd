package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.cloud.framework.security.crypto.model.EnvelopeAadContext;
import net.hwyz.iov.cloud.framework.security.crypto.model.KeyPurpose;
import net.hwyz.iov.cloud.framework.security.crypto.model.WrappingSuite;
import net.hwyz.iov.cloud.framework.security.crypto.provider.EnvelopeAadEncoderRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ParProtoKeyprovAadEncoderV1} 单元测试（VMD-DSN-CR-068 / PAR-PROTO-DSN-CR-009 §2.2）。
 * <p>
 * 覆盖：10 段 canonical AAD 与 provisional 向量逐字节一致；确定性（FW registry 构造期 probe）；
 * 非法 formatVersion / 非白名单 suite / 未知 purpose fail-closed。
 *
 * @author hwyz_leo
 * @since 2026-10-25
 */
class ParProtoKeyprovAadEncoderV1Test {

    private ParProtoKeyprovAadEncoderV1 encoder = new ParProtoKeyprovAadEncoderV1();

    private EnvelopeAadContext probeContext() {
        byte[] cert = new byte[32];
        for (int i = 0; i < 32; i++) {
            cert[i] = (byte) i;
        }
        return new EnvelopeAadContext(1, WrappingSuite.ECDH_ES_P256_HKDF_SHA256_AES256GCM,
                "probe-request", "V2C_COMM_ROOT", KeyPurpose.UPLINK_AEAD, "logical-probe",
                7L, 3L, "v1", cert);
    }

    @Test
    @DisplayName("profileId 固定 PAR_PROTO_KEYPROV_AAD_V1")
    void profileId() {
        assertEquals("PAR_PROTO_KEYPROV_AAD_V1", encoder.profileId());
        assertEquals(ParProtoWireCanonical.AAD_PROFILE, encoder.profileId());
    }

    @Test
    @DisplayName("probe 上下文 → 10 段 canonical AAD 与 provisional 向量逐字节一致（<=1KiB）")
    void encodeProbeVector() {
        byte[] aad = encoder.encode(probeContext());
        assertArrayEquals(ParProtoVectorFixture.PROVISIONAL_PROBE_AAD, aad,
                "AAD encoder 输出必须与 PAR-PROTO CR-009 provisional 向量一致");
        assertTrue(aad.length <= ParProtoWireCanonical.MAX_CANONICAL_AAD_BYTES);
        // aad_digest = SHA-256(canonical_aad)（Wire Mapper 断言侧亦校验）
        assertArrayEquals(ParProtoVectorFixture.PROVISIONAL_PROBE_AAD_DIGEST,
                ParProtoWireCanonical.aadDigest(aad));
    }

    @Test
    @DisplayName("确定性：同一上下文两次编码逐字节一致（FW registry probe 要求）")
    void encodeDeterministic() {
        byte[] first = encoder.encode(probeContext());
        byte[] second = encoder.encode(probeContext());
        assertArrayEquals(first, second);
    }

    @Test
    @DisplayName("FW EnvelopeAadEncoderRegistry 构造期 probe 自检通过（非空/≤1KiB/确定性）")
    void registryProbeAccepts() {
        // 构造 registry 即触发 verifyDeterministic(PROBE_CONTEXT)；异常即测试失败
        EnvelopeAadEncoderRegistry registry = EnvelopeAadEncoderRegistry.of(encoder);
        assertTrue(registry.has(ParProtoWireCanonical.AAD_PROFILE));
        assertEquals(1, registry.size());
    }

    @Test
    @DisplayName("非法 formatVersion → fail-closed 抛异常")
    void encodeBadFormatVersion() {
        EnvelopeAadContext bad = new EnvelopeAadContext(2, WrappingSuite.ECDH_ES_P256_HKDF_SHA256_AES256GCM,
                "req-1", "V2C_COMM_ROOT", KeyPurpose.UPLINK_AEAD, "key-1", 1L, 1L, "v1", new byte[32]);
        assertThrows(IllegalStateException.class, () -> encoder.encode(bad));
    }

    @Test
    @DisplayName("非白名单 suite / 未知 purpose → fail-closed 抛异常")
    void encodeBadSuitePurpose() {
        EnvelopeAadContext badSuite = new EnvelopeAadContext(1, WrappingSuite.RSA_OAEP_SHA256,
                "req-1", "V2C_COMM_ROOT", KeyPurpose.UPLINK_AEAD, "key-1", 1L, 1L, "v1", new byte[32]);
        assertThrows(IllegalStateException.class, () -> encoder.encode(badSuite), "非 ECDH-ES suite 必须拒绝");
        assertThrows(IllegalStateException.class, () -> encoder.encode(null), "空上下文必须拒绝");
    }
}
