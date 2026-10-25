package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import net.hwyz.iov.security.keyprov.v1.KeyProvisioningResponse;
import net.hwyz.iov.security.keyprov.v1.KeyProvisioningResult;
import net.hwyz.iov.security.keyprov.v1.WrappedKeyItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link Kl1WireCodec} 单元测试（PAR-PROTO-DSN-CR-010 §8.6.2 / VMD-DSN-CR-069 §2）。
 * <p>
 * 覆盖：C 提取（仅移除唯一外层 field4）、immutable_content_digest 续签不变、delivery_payload_digest
 * 随 E 变化、受限 field4 patch（其余 bytes 原样）、多重 field4 / 错误 wire type / varint 溢出 / 越界拒绝。
 *
 * @author hwyz_leo
 * @since 2026-10-25
 */
class Kl1WireCodecTest {

    private static final long E1 = 1_800_000_000_000L + 60_000L;
    private static final long E2 = 1_800_000_150_000L;

    private byte[] responseWire(long expireAtMs) {
        return responseWireWithKeyId(expireAtMs, "V2C-KEY-0001");
    }

    private byte[] responseWireWithKeyId(long expireAtMs, String keyId) {
        WrappedKeyItem item = WrappedKeyItem.newBuilder()
                .setKeyId(keyId)
                .setBusinessKeyVersion(1)
                .setWrappedKey(com.google.protobuf.ByteString.copyFrom(new byte[]{1, 2, 3, 4}))
                .setKeyAlgorithm("AES-256")
                .setEnvelopeDigest(com.google.protobuf.ByteString.copyFrom(new byte[32]))
                .build();
        return KeyProvisioningResponse.newBuilder()
                .setRequestId("KL1-REQ-0001")
                .setResult(KeyProvisioningResult.KEY_PROVISIONING_RESULT_SUCCEEDED)
                .addItems(item)
                .setExpireAtMs(expireAtMs)
                .build()
                .toByteArray();
    }

    @Test
    @DisplayName("C 提取：移除唯一外层 field4，其余字段/顺序/unknown bytes 保留")
    void contentCKeepsOtherFields() {
        byte[] wire = responseWire(E1);
        byte[] c = Kl1WireCodec.contentC(wire);
        // C 长度 = 原长度 - (field4 tag 1B + varint 长度)
        assertTrue(c.length < wire.length);
        // C 中不应再含 field4 tag：重放拼接 tag + varint 后应可解析出原内容
        byte[] repaired = concat(c, new byte[]{0x20}, Kl1WireCodec.varint(E1));
        KeyProvisioningResponse parsed;
        try {
            parsed = KeyProvisioningResponse.parseFrom(repaired);
        } catch (Exception e) {
            throw new AssertionError("C + field4 重拼应可解析: " + e.getMessage(), e);
        }
        assertEquals("KL1-REQ-0001", parsed.getRequestId());
        assertEquals(1, parsed.getItemsCount());
        assertEquals("V2C-KEY-0001", parsed.getItems(0).getKeyId());
        assertEquals(E1, parsed.getExpireAtMs());
    }

    @Test
    @DisplayName("immutable_content_digest：续签只改 field4 → digest 不变；改 items → digest 变化")
    void immutableDigestStableAcrossExpirePatch() {
        byte[] wire1 = responseWire(E1);
        byte[] wire2 = Kl1WireCodec.patchField4(wire1, E2);
        assertTrue(wire1.length != wire2.length || !Arrays.equals(wire1, wire2), "续签后 bytes 应变化（E 变）");
        byte[] d1 = Kl1WireCodec.immutableContentDigest(wire1);
        byte[] d2 = Kl1WireCodec.immutableContentDigest(wire2);
        assertArrayEquals(d1, d2, "续签后 immutable_content_digest 必须相等");
        // 内容字节变化（换 keyId）→ digest 变化
        byte[] mutated = responseWireWithKeyId(E1, "V2C-KEY-0002");
        assertFalse(Arrays.equals(d1, Kl1WireCodec.immutableContentDigest(mutated)),
                "内容变化后 immutable_content_digest 必须不同");
    }

    @Test
    @DisplayName("delivery_payload_digest：随本次完整 bytes 变化（含 E）")
    void deliveryDigestChanges() {
        byte[] wire1 = responseWire(E1);
        byte[] wire2 = Kl1WireCodec.patchField4(wire1, E2);
        assertFalse(Arrays.equals(Kl1WireCodec.deliveryPayloadDigest(wire1),
                Kl1WireCodec.deliveryPayloadDigest(wire2)));
    }

    @Test
    @DisplayName("受限 patch：仅 field4 value 变化，其余字节原样")
    void patchOnlyField4() {
        byte[] wire = responseWire(E1);
        byte[] patched = Kl1WireCodec.patchField4(wire, E2);
        KeyProvisioningResponse parsed;
        try {
            parsed = KeyProvisioningResponse.parseFrom(patched);
        } catch (Exception e) {
            throw new AssertionError(e.getMessage(), e);
        }
        assertEquals(E2, parsed.getExpireAtMs(), "新 E 生效");
        assertEquals("KL1-REQ-0001", parsed.getRequestId());
        assertEquals("V2C-KEY-0001", parsed.getItems(0).getKeyId());
        // 非 field4 字节完全保留（除被替换的 varint 区间外）
        assertArrayEquals(Kl1WireCodec.contentC(wire), Kl1WireCodec.contentC(patched));
    }

    @Test
    @DisplayName("畸形 wire：多重 field4 拒绝")
    void rejectMultipleField4() {
        byte[] wire = responseWire(E1);
        // 手工附加第二个 field4 tag + varint
        byte[] dup = concat(wire, new byte[]{0x20, 0x05});
        assertThrows(IllegalArgumentException.class, () -> Kl1WireCodec.locateField4(dup));
        assertThrows(IllegalArgumentException.class, () -> Kl1WireCodec.patchField4(dup, E2));
    }

    @Test
    @DisplayName("畸形 wire：field4 wire type 非 varint 拒绝")
    void rejectWrongWireType() {
        byte[] wire = responseWire(E1);
        // 手工构造：先 field4 用 length-delimited wire type（tag 0x22）
        byte[] malformed = concat(new byte[]{0x22, 0x02, 0x01, 0x02}, wire);
        assertThrows(IllegalArgumentException.class, () -> Kl1WireCodec.locateField4(malformed));
    }

    @Test
    @DisplayName("畸形 wire：varint 溢出（第10字节高4位非0）拒绝")
    void rejectVarintOverflow() {
        byte[] wire = responseWire(E1);
        // 手工构造：10 字节 varint，第 10 字节 0xFF（高4位非0）
        byte[] overflow = concat(new byte[]{0x20,
                (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80,
                (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0xFF}, wire);
        assertThrows(IllegalArgumentException.class, () -> Kl1WireCodec.locateField4(overflow));
    }

    @Test
    @DisplayName("畸形 wire：varint 越界（截断）拒绝")
    void rejectTruncatedVarint() {
        byte[] wire = responseWire(E1);
        byte[] truncated = Arrays.copyOf(wire, wire.length - 1);
        assertThrows(IllegalArgumentException.class, () -> Kl1WireCodec.contentC(truncated));
    }

    @Test
    @DisplayName("缺 field4：仅允许已批准历史 codec 迁移，KL1 原响应必须含 field4")
    void rejectMissingField4() {
        // 构造不含 expire_at_ms 的响应（proto3 零值不序列化）
        byte[] noField4 = KeyProvisioningResponse.newBuilder()
                .setRequestId("R")
                .setResult(KeyProvisioningResult.KEY_PROVISIONING_RESULT_SUCCEEDED)
                .build().toByteArray();
        assertThrows(IllegalArgumentException.class, () -> Kl1WireCodec.locateField4(noField4));
    }

    @Test
    @DisplayName("domain 分隔：immutable digest 输入 = ASCII(domain) || 0x00 || C")
    void digestDomainSeparation() {
        byte[] wire = responseWire(E1);
        byte[] c = Kl1WireCodec.contentC(wire);
        byte[] domain = Kl1Constants.CONTENT_DIGEST_DOMAIN.getBytes(StandardCharsets.US_ASCII);
        byte[] input = new byte[domain.length + 1 + c.length];
        System.arraycopy(domain, 0, input, 0, domain.length);
        input[domain.length] = 0x00;
        System.arraycopy(c, 0, input, domain.length + 1, c.length);
        assertArrayEquals(Kl1WireCodec.sha256(input), Kl1WireCodec.immutableContentDigest(wire));
    }

    private static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] p : parts) {
            out.writeBytes(p);
        }
        return out.toByteArray();
    }
}
