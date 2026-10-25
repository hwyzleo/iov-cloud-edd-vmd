package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

/**
 * KL1 内容摘要与受限 wire patch 核心（PAR-PROTO-DSN-CR-010 §8.6.2 / VMD-DSN-CR-069 §2）。
 * <p>
 * 对已校验的原 Response wire bytes：
 * <ul>
 *   <li>C = 移除<strong>唯一外层 field4</strong>（KeyProvisioningResponse.expire_at_ms，wire type 0 varint）
 *       完整 tag + varint，保留其余 tag/value/顺序/unknown bytes 得到的字节串；</li>
 *   <li>{@code immutable_content_digest = SHA256(ASCII("openiov|keyprov-content|kl1") || 0x00 || C)}
 *       （每次续签后必须相等）；</li>
 *   <li>{@code delivery_payload_digest = SHA256(本次完整 Response wire bytes)}（含本次 E，每次变化）。</li>
 * </ul>
 * 禁止通过 domain-map → protobuf 重建/重序列化 items 后计算 C。
 * <p>
 * patch 器只替换唯一外层 field4 的 value；完整保存所有非 field4 的原始 tag+value 与 unknown 字段。
 * 拒绝多重 field4、错误 wire type、varint 溢出/畸形长度；原缺失 field4 只走已批准历史 codec 迁移。
 *
 * @author hwyz_leo
 * @since 2026-10-25
 */
public final class Kl1WireCodec {

    private Kl1WireCodec() {
    }

    /** KeyProvisioningResponse.field4 = expire_at_ms（int64 varint）：tag = (4 << 3) | 0 = 0x20 */
    private static final int FIELD4_TAG_BYTE = 0x20;

    /** varint 最大字节数（protobuf：10 字节，第 10 字节高 4 位必须为 0） */
    private static final int MAX_VARINT_BYTES = 10;

    /**
     * 定位唯一外层 field4 的完整 tag + varint 字节区间 [tagStart, valueEnd)。
     * 同时校验整个顶层 wire 可解析且 field4 唯一、wire type 正确。
     *
     * @throws IllegalArgumentException 畸形 wire / 多重 field4 / 错误 wire type / 缺失 field4
     */
    static Slice locateField4(byte[] wire) {
        if (wire == null || wire.length == 0) {
            throw new IllegalArgumentException("KL1: Response wire 为空");
        }
        int index = 0;
        Integer tagStart = null;
        Integer valueEnd = null;
        while (index < wire.length) {
            int tagStartCurrent = index;
            long tag = readVarint(wire, index);
            int tagLen = varintLen(wire, index);
            index += tagLen;
            int fieldNumber = (int) (tag >>> 3);
            int wireType = (int) (tag & 0x07);
            if (fieldNumber == 4) {
                if (wireType != 0) {
                    throw new IllegalArgumentException("KL1: field4 wire type 非法（须 varint）: " + wireType);
                }
                if (tagStart != null) {
                    throw new IllegalArgumentException("KL1: 多重 field4，拒绝 patch");
                }
                tagStart = tagStartCurrent;
                long ignored = readVarint(wire, index);
                int len = varintLen(wire, index);
                if (ignored < 0) {
                    throw new IllegalArgumentException("KL1: field4 值为负，非法");
                }
                index += len;
                valueEnd = index;
            } else {
                index = skipField(wire, index, wireType, tagStartCurrent);
            }
        }
        if (tagStart == null || valueEnd == null) {
            throw new IllegalArgumentException("KL1: 原响应缺失唯一外层 field4（仅允许已批准历史 codec 迁移）");
        }
        return new Slice(tagStart, valueEnd);
    }

    /**
     * 计算内容字节 C：移除唯一外层 field4 完整 tag + varint 后的原始字节串（保序，含 unknown 字段）。
     */
    static byte[] contentC(byte[] wire) {
        Slice s = locateField4(wire);
        byte[] out = new byte[wire.length - (s.valueEnd - s.tagStart)];
        System.arraycopy(wire, 0, out, 0, s.tagStart);
        System.arraycopy(wire, s.valueEnd, out, s.tagStart, wire.length - s.valueEnd);
        return out;
    }

    /**
     * immutable_content_digest = SHA256(ASCII(domain) || 0x00 || C)。
     * 刷新 E 后必须保持不变。
     */
    static byte[] immutableContentDigest(byte[] wire) {
        byte[] c = contentC(wire);
        byte[] domain = Kl1Constants.CONTENT_DIGEST_DOMAIN.getBytes(StandardCharsets.US_ASCII);
        byte[] input = new byte[domain.length + 1 + c.length];
        System.arraycopy(domain, 0, input, 0, domain.length);
        input[domain.length] = 0x00;
        System.arraycopy(c, 0, input, domain.length + 1, c.length);
        return sha256(input);
    }

    /**
     * delivery_payload_digest = SHA256(本次完整 Response wire bytes)。
     */
    static byte[] deliveryPayloadDigest(byte[] fullWire) {
        return sha256(fullWire);
    }

    /**
     * 受限 patch：仅替换唯一外层 field4 的完整 tag + varint 为新 E，其余 bytes 原样保留。
     */
    static byte[] patchField4(byte[] wire, long newExpireAtMs) {
        if (newExpireAtMs < 0) {
            throw new IllegalArgumentException("KL1: 新 E 必须非负: " + newExpireAtMs);
        }
        Slice s = locateField4(wire);
        byte[] newValue = varint(newExpireAtMs);
        byte[] tag = varint(4L << 3); // wire type 0, field 4 → 0x20
        byte[] out = new byte[wire.length - (s.valueEnd - s.tagStart) + tag.length + newValue.length];
        System.arraycopy(wire, 0, out, 0, s.tagStart);
        System.arraycopy(tag, 0, out, s.tagStart, tag.length);
        System.arraycopy(newValue, 0, out, s.tagStart + tag.length, newValue.length);
        System.arraycopy(wire, s.valueEnd, out, s.tagStart + tag.length + newValue.length, wire.length - s.valueEnd);
        return out;
    }

    /** 读取 varint 值（unsigned 语义，第 10 字节高 4 位必须为 0）。 */
    private static long readVarint(byte[] wire, int start) {
        int len = varintLen(wire, start);
        long result = 0;
        for (int i = 0; i < len; i++) {
            result |= (long) (wire[start + i] & 0x7F) << (7 * i);
        }
        return result;
    }

    /** 计算从 start 开始的 varint 字节数；溢出/越界/畸形抛出 IllegalArgumentException。 */
    private static int varintLen(byte[] wire, int start) {
        int i = 0;
        while (i < MAX_VARINT_BYTES) {
            int pos = start + i;
            if (pos >= wire.length) {
                throw new IllegalArgumentException("KL1: varint 越界（畸形 wire）");
            }
            if ((wire[pos] & 0x80) == 0) {
                if (i == MAX_VARINT_BYTES - 1 && (wire[pos] & 0xF0) != 0) {
                    throw new IllegalArgumentException("KL1: varint 溢出（第10字节高4位非0）");
                }
                return i + 1;
            }
            i++;
        }
        throw new IllegalArgumentException("KL1: varint 超过 10 字节（畸形）");
    }

    /** 编码 unsigned varint（int64 非负值标准 protobuf 编码）。 */
    static byte[] varint(long value) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(9);
        long remaining = value;
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

    /** 跳过非 field4 字段的 payload；group wire type 直接拒绝（KeyProvisioningResponse 无 group 字段）。 */
    private static int skipField(byte[] wire, int index, int wireType, int tagStart) {
        switch (wireType) {
            case 0: {
                int len = varintLen(wire, index);
                return index + len;
            }
            case 1: {
                checkBounds(wire, index, 8, tagStart);
                return index + 8;
            }
            case 2: {
                long length = readVarint(wire, index);
                int len = varintLen(wire, index);
                if (length < 0 || length > Integer.MAX_VALUE - index - len) {
                    throw new IllegalArgumentException("KL1: length-delimited 长度非法");
                }
                checkBounds(wire, index + len, (int) length, tagStart);
                return index + len + (int) length;
            }
            case 5: {
                checkBounds(wire, index, 4, tagStart);
                return index + 4;
            }
            default:
                throw new IllegalArgumentException("KL1: 不支持 wire type（group/非法）: " + wireType);
        }
    }

    private static void checkBounds(byte[] wire, int offset, int length, int tagStart) {
        if (offset < 0 || length < 0 || offset + length > wire.length) {
            throw new IllegalArgumentException("KL1: 字段数据越界（畸形 wire），tagStart=" + tagStart);
        }
    }

    static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    /**
     * 扫描顶层 length-delimited 字段（field3 = WrappedKeyItem items）的 body 切片。
     * 用于记录每用途 item 在原响应中的无损切片证据（offset/length）。
     */
    static List<BodySlice> locateField3Items(byte[] wire) {
        if (wire == null || wire.length == 0) {
            return java.util.Collections.emptyList();
        }
        List<BodySlice> out = new java.util.ArrayList<>();
        int index = 0;
        while (index < wire.length) {
            long tag = readVarint(wire, index);
            int tagLen = varintLen(wire, index);
            index += tagLen;
            int fieldNumber = (int) (tag >>> 3);
            int wireType = (int) (tag & 0x07);
            if (fieldNumber == 3 && wireType == 2) {
                long length = readVarint(wire, index);
                int len = varintLen(wire, index);
                checkBounds(wire, index + len, (int) length, 0);
                index += len;
                out.add(new BodySlice(index, (int) length));
                index += (int) length;
            } else {
                index = skipField(wire, index, wireType, 0);
            }
        }
        return out;
    }

    /** field4 完整 tag + varint 的字节区间。 */
    static final class Slice {
        final int tagStart;
        final int valueEnd;

        Slice(int tagStart, int valueEnd) {
            this.tagStart = tagStart;
            this.valueEnd = valueEnd;
        }

        @Override
        public String toString() {
            return "Slice[tagStart=" + tagStart + ", valueEnd=" + valueEnd + "]";
        }
    }

    /** length-delimited 字段 body 切片（offset 指向内容起点，length 为内容长度）。 */
    static final class BodySlice {
        final int offset;
        final int length;

        BodySlice(int offset, int length) {
            this.offset = offset;
            this.length = length;
        }

        @Override
        public String toString() {
            return "BodySlice[offset=" + offset + ", length=" + length + "]";
        }
    }
}
