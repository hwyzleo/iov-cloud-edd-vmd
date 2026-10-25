package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * PAR-PROTO CR-009 V2C Keyprov 向量 fixture（VMD-DSN-CR-068 §7.1）。
 * <p>
 * <strong>向量权威边界（用户评审 2026-10-25）</strong>：
 * <ul>
 *   <li>PAR-PROTO 仓向量（{@code verify/keyprov-ecdh-es-p256-v1/}）是<strong>唯一权威源</strong>；
 *       本仓 fixture 仅是受控镜像 / 预发布快照，不得独立定义输入、字段编码或期望结果。</li>
 *   <li>本 fixture 固定输入取自 CR-009 已冻结 registry 规则，期望摘要为<strong>provisional</strong>
 *       （由本规范自算），只用于开发/单元回归，<strong>不作为 capability READY 或生产放行证据</strong>。</li>
 *   <li>{@link #vectorMatched()} 仅认 vendored 官方向量（classpath
 *       {@code par-proto/verify/keyprov-ecdh-es-p256-v1/wire_envelope_digest.hex}），
 *       未 vendored 或比对不一致一律 false → keyprov Health 的 PAR_PROTO_CR009_VECTOR_MATCHED 保持 DOWN。</li>
 *   <li>PAR-PROTO 正式向量可用后，CI 必须直接消费/逐字节比对，存在差异立即失败。</li>
 * </ul>
 * provenance（provisional）：
 * <pre>
 *   PAR-PROTO release      : PAR-PROTO CR-009（provisional，正式 release 待发布）
 *   PAR-PROTO commit       : 未锁定（正式向量 vendored 后回填）
 *   vector manifest hash   : provisional（随官方向量包更新）
 *   profile ID             : PAR_PROTO_KEYPROV_ECDH_ES_P256_V1 / PAR_PROTO_KEYPROV_AAD_V1
 *   vector schema/version  : v1
 * </pre>
 *
 * @author hwyz_leo
 * @since 2026-10-25
 */
public final class ParProtoVectorFixture {

    private ParProtoVectorFixture() {
    }

    /** 固定输入（CR-009 冻结规则；provisional） */
    public static final int VECTOR_ENVELOPE_FORMAT = 1;
    public static final String VECTOR_REQUEST_ID = "vector-req-0001";
    public static final String VECTOR_BUSINESS_DOMAIN = "V2C_COMM";
    public static final int VECTOR_PURPOSE = 1; // UPLINK_AEAD
    public static final String VECTOR_KEY_ID = "vector-key-uplink";
    public static final long VECTOR_BUSINESS_KEY_VERSION = 7L;
    public static final int VECTOR_WRAPPING_SUITE = 2; // ECDH_ES_P256_HKDF_SHA256_AES256GCM
    public static final byte[] VECTOR_WRAPPED_KEY = new byte[]{1, 2, 3};
    public static final byte[] VECTOR_PARAMETERS = new byte[]{4, 5, 6, 7};

    /** provisional 期望 wire digest（由本规范自算；正式向量 vendored 后以官方向量为准） */
    public static final byte[] PROVISIONAL_WIRE_DIGEST =
            hexDecode("0a7ec9f627b9eff7598912f4623f9472a984a65e4ca66d06d8b36cfd02f10107");

    /** AAD probe 上下文期望（与 FW {@code EnvelopeAadEncoderRegistry} PROBE_CONTEXT 对齐） */
    public static final byte[] PROVISIONAL_PROBE_AAD =
            hexDecode("000000010100000001020000000101000000080000000000000007000000080000000000000003"
                    + "00000020000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"
                    + "0000000d70726f62652d726571756573740000000d5632435f434f4d4d5f524f4f540000000d6c6f676963616c2d70726f6265000000027631");
    public static final byte[] PROVISIONAL_PROBE_AAD_DIGEST =
            hexDecode("31ec86d966bb42558b7db6661cbc82b083433d081282e74b68fa677922906b89");

    /** vendored 官方向量资源路径（权威源；未放置时 vectorMatched=false） */
    private static final String OFFICIAL_VECTOR_RESOURCE =
            "/par-proto/verify/keyprov-ecdh-es-p256-v1/wire_envelope_digest.hex";

    /**
     * PAR-PROTO CR-009 官方向量是否已 vendored 且与 Wire Mapper 输出一致。
     * <p>仅认 classpath 官方向量 hex 文件；provisional fixture 不参与 READY 判定。
     */
    public static boolean vectorMatched() {
        String officialHex;
        try (InputStream in = ParProtoVectorFixture.class.getResourceAsStream(OFFICIAL_VECTOR_RESOURCE)) {
            if (in == null) {
                return false;
            }
            officialHex = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            return false;
        }
        byte[] official = hexDecode(officialHex);
        byte[] computed = ParProtoWireCanonical.wireEnvelopeDigest(
                VECTOR_ENVELOPE_FORMAT, VECTOR_REQUEST_ID, VECTOR_BUSINESS_DOMAIN,
                VECTOR_PURPOSE, VECTOR_KEY_ID, VECTOR_BUSINESS_KEY_VERSION,
                VECTOR_WRAPPING_SUITE, VECTOR_WRAPPED_KEY, VECTOR_PARAMETERS);
        return java.util.Arrays.equals(official, computed);
    }

    private static byte[] hexDecode(String hex) {
        String clean = hex.trim();
        byte[] out = new byte[clean.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(clean.charAt(i * 2), 16);
            int lo = Character.digit(clean.charAt(i * 2 + 1), 16);
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }
}
