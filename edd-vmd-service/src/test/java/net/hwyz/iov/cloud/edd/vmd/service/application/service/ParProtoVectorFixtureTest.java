package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ParProtoVectorFixture} 测试（VMD-DSN-CR-068 §7.1 向量边界）。
 * <p>
 * 向量权威边界：PAR-PROTO 仓向量为唯一权威源；本仓 fixture 为受控镜像/预发布快照（provisional），
 * 仅用于开发/单元回归，不作为 capability READY 或生产放行证据。{@link ParProtoVectorFixture#vectorMatched()}
 * 仅认 vendored 官方向量（classpath {@code par-proto/verify/keyprov-ecdh-es-p256-v1/wire_envelope_digest.hex}）。
 *
 * @author hwyz_leo
 * @since 2026-10-25
 */
class ParProtoVectorFixtureTest {

    @Test
    @DisplayName("provisional wire digest 与 Wire Mapper 计算一致（防回归；正式向量 vendored 后以官方为准）")
    void provisionalWireDigestConsistent() {
        byte[] computed = ParProtoWireCanonical.wireEnvelopeDigest(
                ParProtoVectorFixture.VECTOR_ENVELOPE_FORMAT,
                ParProtoVectorFixture.VECTOR_REQUEST_ID,
                ParProtoVectorFixture.VECTOR_BUSINESS_DOMAIN,
                ParProtoVectorFixture.VECTOR_PURPOSE,
                ParProtoVectorFixture.VECTOR_KEY_ID,
                ParProtoVectorFixture.VECTOR_BUSINESS_KEY_VERSION,
                ParProtoVectorFixture.VECTOR_WRAPPING_SUITE,
                ParProtoVectorFixture.VECTOR_WRAPPED_KEY,
                ParProtoVectorFixture.VECTOR_PARAMETERS);
        assertArrayEquals(ParProtoVectorFixture.PROVISIONAL_WIRE_DIGEST, computed);
    }

    @Test
    @DisplayName("vectorMatched：test classpath 已 vendored 官方向量镜像 → 比对通过")
    void vectorMatchedWhenVendored() {
        assertTrue(ParProtoVectorFixture.vectorMatched(),
                "test resources 提供官方向量镜像时 vectorMatched 必须通过");
    }

    @Test
    @DisplayName("provisional probe AAD 与 AAD Encoder 输出一致（10 段 registry）")
    void provisionalProbeAadConsistent() {
        ParProtoKeyprovAadEncoderV1 encoder = new ParProtoKeyprovAadEncoderV1();
        byte[] cert = new byte[32];
        for (int i = 0; i < 32; i++) {
            cert[i] = (byte) i;
        }
        net.hwyz.iov.cloud.framework.security.crypto.model.EnvelopeAadContext ctx =
                new net.hwyz.iov.cloud.framework.security.crypto.model.EnvelopeAadContext(1,
                        net.hwyz.iov.cloud.framework.security.crypto.model.WrappingSuite.ECDH_ES_P256_HKDF_SHA256_AES256GCM,
                        "probe-request", "V2C_COMM_ROOT",
                        net.hwyz.iov.cloud.framework.security.crypto.model.KeyPurpose.UPLINK_AEAD,
                        "logical-probe", 7L, 3L, "v1", cert);
        assertArrayEquals(ParProtoVectorFixture.PROVISIONAL_PROBE_AAD, encoder.encode(ctx));
    }
}
