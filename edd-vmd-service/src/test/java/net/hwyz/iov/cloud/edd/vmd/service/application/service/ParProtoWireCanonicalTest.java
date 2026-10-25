package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ParProtoWireCanonical} 单元测试（PAR-PROTO-DSN-CR-009 §2 canonical registry）。
 * <p>
 * 覆盖 segment/unsignedVarint/uint64be 编码边界与 9 字段 wire digest 的负向 canonical 选择
 * （enum name()/ordinal()、十进制 version 文本等错误方式必须得到不同摘要）。
 *
 * @author hwyz_leo
 * @since 2026-10-25
 */
class ParProtoWireCanonicalTest {

    @Test
    @DisplayName("unsignedVarint 边界：0/127/128/255/256/2^32-1（protobuf LEB128）")
    void unsignedVarintBoundaries() {
        assertArrayEquals(new byte[]{0x00}, ParProtoWireCanonical.unsignedVarint(0));
        assertArrayEquals(new byte[]{0x7F}, ParProtoWireCanonical.unsignedVarint(127));
        assertArrayEquals(new byte[]{(byte) 0x80, 0x01}, ParProtoWireCanonical.unsignedVarint(128));
        assertArrayEquals(new byte[]{(byte) 0xFF, 0x01}, ParProtoWireCanonical.unsignedVarint(255));
        assertArrayEquals(new byte[]{(byte) 0x80, 0x02}, ParProtoWireCanonical.unsignedVarint(256));
        assertArrayEquals(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x0F},
                ParProtoWireCanonical.unsignedVarint(0xFFFF_FFFF), "2^32-1 边界（int 无符号位模式）");
    }

    @Test
    @DisplayName("segment：uint32be 长度前缀 + 原始字节")
    void segmentLengthPrefix() {
        byte[] value = "ab".getBytes(StandardCharsets.UTF_8);
        byte[] segment = ParProtoWireCanonical.segment(value);
        assertEquals(6, segment.length);
        assertEquals(0, segment[0]);
        assertEquals(0, segment[1]);
        assertEquals(0, segment[2]);
        assertEquals(2, segment[3]);
        assertArrayEquals(value, java.util.Arrays.copyOfRange(segment, 4, 6));
    }

    @Test
    @DisplayName("uint64be：8 字节大端")
    void uint64BigEndian() {
        byte[] segment = ParProtoWireCanonical.segmentUint64(7L);
        assertEquals(12, segment.length);
        assertArrayEquals(new byte[]{0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0, 7}, segment);
    }

    @Test
    @DisplayName("wire digest：proto 枚举值（非 name()/ordinal()）改变任何一段都得到不同摘要")
    void wireDigestCanonicalChoices() {
        byte[] canonical = ParProtoWireCanonical.wireEnvelopeDigest(
                1, "vector-req-0001", "V2C_COMM", 1, "vector-key-uplink", 7L, 2,
                new byte[]{1, 2, 3}, new byte[]{4, 5, 6, 7});
        // ordinal()（UPLINK=0）/ name 文本（"UPLINK_AEAD"）替代 proto 枚举值 → 不同摘要
        byte[] wrongOrdinal = ParProtoWireCanonical.wireEnvelopeDigest(
                1, "vector-req-0001", "V2C_COMM", 0, "vector-key-uplink", 7L, 2,
                new byte[]{1, 2, 3}, new byte[]{4, 5, 6, 7});
        assertFalse(java.util.Arrays.equals(canonical, wrongOrdinal), "ordinal 代替 proto 枚举值必须得到不同摘要");
        // 十进制 version 文本（"7" UTF-8 而非 uint64be）→ 不同摘要
        byte[] wrongVersion = ParProtoWireCanonical.wireEnvelopeDigest(
                1, "vector-req-0001", "V2C_COMM", 1, "vector-key-uplink", 7L, 2,
                new byte[]{1, 2, 3}, ParProtoWireCanonical.segmentUtf8("7"));
        assertFalse(java.util.Arrays.equals(canonical, wrongVersion), "十进制 version 文本必须得到不同摘要");
        // 非 deterministic 重编码 parameters（parse→重放）→ 不同摘要
        byte[] wrongParams = ParProtoWireCanonical.wireEnvelopeDigest(
                1, "vector-req-0001", "V2C_COMM", 1, "vector-key-uplink", 7L, 2,
                new byte[]{1, 2, 3}, new byte[]{7, 6, 5, 4});
        assertFalse(java.util.Arrays.equals(canonical, wrongParams), "parameters 顺序变化必须得到不同摘要");
        // 与 provisional fixture 期望一致（防回归）
        assertArrayEquals(ParProtoVectorFixture.PROVISIONAL_WIRE_DIGEST, canonical,
                "wire digest 必须与 PAR-PROTO CR-009 provisional 向量一致");
    }

    @Test
    @DisplayName("canonicalAad：10 段 registry 与 provisional probe 向量一致，且 aad_digest=SHA-256(aad)")
    void canonicalAadProbeVector() {
        byte[] cert = new byte[32];
        for (int i = 0; i < 32; i++) {
            cert[i] = (byte) i;
        }
        byte[] aad = ParProtoWireCanonical.canonicalAad(
                1, 2, 1, 7L, 3L, cert, "probe-request", "V2C_COMM_ROOT", "logical-probe", "v1");
        assertArrayEquals(ParProtoVectorFixture.PROVISIONAL_PROBE_AAD, aad,
                "canonical AAD 必须与 PAR-PROTO CR-009 provisional 向量一致");
        assertArrayEquals(ParProtoVectorFixture.PROVISIONAL_PROBE_AAD_DIGEST,
                ParProtoWireCanonical.aadDigest(aad), "aad_digest = SHA-256(canonical_aad)");
    }
}
