package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * PAR-PROTO CR-009 V2C Keyprov wire canonical 编码 helper（VMD-DSN-CR-068 / PAR-PROTO-DSN-CR-009 §2）。
 * <p>
 * 仅实现 PAR-PROTO 权威 registry：AAD profile（10 段）与 wire digest（9 段）的 segment 编码。
 * 规则（来自 PAR-PROTO-DSN-CR-009 §2 与 proto 源注释）：
 * <pre>
 *   segment(x) = uint32be(len(x)) || x
 *   unsignedVarint = protobuf LEB128 varint（禁止 enum name()/ordinal()/十进制）
 *   uint64be = 8 字节大端
 *   wireDigestInput = segment(envelopeFormat) ‖ segment(UTF8(requestId)) ‖ segment(UTF8(businessDomain))
 *                    ‖ segment(unsignedVarint(purpose)) ‖ segment(UTF8(keyId))
 *                    ‖ segment(uint64be(businessKeyVersion)) ‖ segment(unsignedVarint(wrappingSuite))
 *                    ‖ segment(wrappedKey) ‖ segment(originalDeterministicParametersBytes)
 *   wireEnvelopeDigest = SHA-256(wireDigestInput)
 *   canonicalAad = segment(envelopeFormat) ‖ segment(wrappingSuite) ‖ segment(purpose)
 *                 ‖ segment(uint64be(businessKeyVersion)) ‖ segment(uint64be(rootKeyVersion))
 *                 ‖ segment(recipientCertificateDigest) ‖ segment(UTF8(requestId))
 *                 ‖ segment(UTF8(businessDomain)) ‖ segment(UTF8(keyId)) ‖ segment(UTF8(derivationVersion))
 *   aadDigest = SHA-256(canonicalAad)
 * </pre>
 * 本 helper 不依赖业务密钥、不解析 FW 私有 envelopeBytes、不手写 Protobuf 字段编码。
 * 生产代码只允许通过本类输出 canonical AAD / wire digest input；测试 fixture 为受控镜像（见
 * {@code ParProtoVectorFixture}，provisional，PAR-PROTO 仓向量为唯一权威源）。
 *
 * @author hwyz_leo
 * @since 2026-10-25
 */
final class ParProtoWireCanonical {

    private ParProtoWireCanonical() {
    }

    /** PAR-PROTO wire digest profile（CR-009 §2.1） */
    static final String WIRE_PROFILE = "PAR_PROTO_KEYPROV_ECDH_ES_P256_V1";

    /** PAR-PROTO AAD profile（CR-009 §2.2） */
    static final String AAD_PROFILE = "PAR_PROTO_KEYPROV_AAD_V1";

    /** canonical AAD 最大长度（proto 契约 canonical_aad <= 1 KiB） */
    static final int MAX_CANONICAL_AAD_BYTES = 1024;

    /** canonical AAD 段数（固定 10） */
    static final int CANONICAL_AAD_SEGMENTS = 10;

    /** wire digest 段数（固定 9） */
    static final int WIRE_DIGEST_SEGMENTS = 9;

    /** 段编码：uint32be(len) || bytes */
    static byte[] segment(byte[] value) {
        if (value == null) {
            value = new byte[0];
        }
        if (value.length > 0xFFFF_FFFFL) {
            throw new IllegalStateException("PAR-PROTO segment 超长: " + value.length);
        }
        byte[] out = new byte[4 + value.length];
        writeUint32Be(out, 0, value.length);
        System.arraycopy(value, 0, out, 4, value.length);
        return out;
    }

    /** UTF-8 字符串段 */
    static byte[] segmentUtf8(String value) {
        return segment(value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8));
    }

    /** unsigned varint（protobuf LEB128）段；仅接受非负 int 枚举值 */
    static byte[] segmentVarint(int value) {
        if (value < 0) {
            throw new IllegalStateException("PAR-PROTO unsigned varint 不接受负值: " + value);
        }
        return segment(unsignedVarint(value));
    }

    /** uint64be（8 字节大端）段 */
    static byte[] segmentUint64(long value) {
        byte[] bigEndian = new byte[8];
        for (int i = 0; i < 8; i++) {
            bigEndian[7 - i] = (byte) (value >>> (i * 8));
        }
        return segment(bigEndian);
    }

    /** unsigned varint 原始字节（protobuf LEB128，小端组序；int 按无符号 32 位位模式处理） */
    static byte[] unsignedVarint(int value) {
        // int 以无符号位模式参与编码（0xFFFF_FFFF = 4294967295），枚举调用方只传受控正数（1/2）
        long remaining = Integer.toUnsignedLong(value);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(5);
        while (true) {
            if ((remaining & ~0x7FL) == 0) {
                out.write((int) remaining);
                break;
            }
            out.write((int) ((remaining & 0x7F) | 0x80));
            remaining >>>= 7;
        }
        return out.toByteArray();
    }

    /**
     * 计算 PAR-PROTO 9 段 wireEnvelopeDigest。
     *
     * @param envelopeFormatValue proto KeyEnvelopeFormat 枚举值（如 1）
     * @param requestId           requestId（UTF-8）
     * @param businessDomain      业务域（如 V2C_COMM，UTF-8）
     * @param purposeValue        proto KeyPurpose 枚举值（1/2）
     * @param keyId               logicalKeyId（UTF-8）
     * @param businessKeyVersion  业务版本（uint64be）
     * @param wrappingSuiteValue  proto WrappingSuite 枚举值（如 2）
     * @param wrappedKey          ciphertext 原始字节
     * @param parametersBytes     原始 deterministic parameters 字节
     * @return SHA-256(wireDigestInput)
     */
    static byte[] wireEnvelopeDigest(int envelopeFormatValue, String requestId, String businessDomain,
                                     int purposeValue, String keyId, long businessKeyVersion,
                                     int wrappingSuiteValue, byte[] wrappedKey, byte[] parametersBytes) {
        byte[] input = wireDigestInput(envelopeFormatValue, requestId, businessDomain,
                purposeValue, keyId, businessKeyVersion, wrappingSuiteValue, wrappedKey, parametersBytes);
        return sha256(input);
    }

    /**
     * 组装 9 段 wire digest input（共且仅有 9 个顶层 segment；顺序固定，禁止重排）。
     */
    static byte[] wireDigestInput(int envelopeFormatValue, String requestId, String businessDomain,
                                  int purposeValue, String keyId, long businessKeyVersion,
                                  int wrappingSuiteValue, byte[] wrappedKey, byte[] parametersBytes) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(256);
        out.writeBytes(segmentVarint(envelopeFormatValue));
        out.writeBytes(segmentUtf8(requestId));
        out.writeBytes(segmentUtf8(businessDomain));
        out.writeBytes(segmentVarint(purposeValue));
        out.writeBytes(segmentUtf8(keyId));
        out.writeBytes(segmentUint64(businessKeyVersion));
        out.writeBytes(segmentVarint(wrappingSuiteValue));
        out.writeBytes(segment(wrappedKey == null ? new byte[0] : wrappedKey));
        out.writeBytes(segment(parametersBytes == null ? new byte[0] : parametersBytes));
        return out.toByteArray();
    }

    /**
     * 组装 PAR-PROTO AAD profile（10 段；顺序固定，禁止重排；长度上限 1 KiB）。
     */
    static byte[] canonicalAad(int envelopeFormatValue, int wrappingSuiteValue, int purposeValue,
                               long businessKeyVersion, long rootKeyVersion, byte[] recipientCertificateDigest,
                               String requestId, String businessDomain, String keyId, String derivationVersion) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(128);
        out.writeBytes(segmentVarint(envelopeFormatValue));
        out.writeBytes(segmentVarint(wrappingSuiteValue));
        out.writeBytes(segmentVarint(purposeValue));
        out.writeBytes(segmentUint64(businessKeyVersion));
        out.writeBytes(segmentUint64(rootKeyVersion));
        out.writeBytes(segment(recipientCertificateDigest == null ? new byte[0] : recipientCertificateDigest));
        out.writeBytes(segmentUtf8(requestId));
        out.writeBytes(segmentUtf8(businessDomain));
        out.writeBytes(segmentUtf8(keyId));
        out.writeBytes(segmentUtf8(derivationVersion));
        byte[] aad = out.toByteArray();
        if (aad.length > MAX_CANONICAL_AAD_BYTES) {
            throw new IllegalStateException("PAR-PROTO canonical AAD 超过 1 KiB: " + aad.length);
        }
        return aad;
    }

    /** aadDigest = SHA-256(canonicalAad) */
    static byte[] aadDigest(byte[] canonicalAad) {
        return sha256(canonicalAad);
    }

    private static void writeUint32Be(byte[] target, int offset, int value) {
        target[offset] = (byte) (value >>> 24);
        target[offset + 1] = (byte) (value >>> 16);
        target[offset + 2] = (byte) (value >>> 8);
        target[offset + 3] = (byte) value;
    }

    static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
