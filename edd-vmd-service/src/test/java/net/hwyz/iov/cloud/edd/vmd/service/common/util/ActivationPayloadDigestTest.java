package net.hwyz.iov.cloud.edd.vmd.service.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 激活事实载荷摘要单元测试（VMD-DSN-CR-077 §4.4 / R-10 / TC-077-18 / 终审修复 B-4 残留）
 * <p>
 * 核心断言：摘要<strong>与 eventId 无关</strong>（同业务事实不同 eventId 摘要一致）、
 * {@code sourceFactRef} 为空按空串参与、可稳定复算；
 * 以及 {@code occurredAt} 文本口径（原文优先 / 毫秒截断 / 亚毫秒兼容窗口）与完整性校验语义。
 *
 * @author hwyz_leo
 */
@DisplayName("ActivationPayloadDigest 测试")
class ActivationPayloadDigestTest {

    private static final String VIN = "LSVAA1234567890AB";
    private static final String OP = "BOP20261122094501000123";
    private static final String REF = "vso:order:SO20261120001#delivery";
    private static final Instant OCCURRED_AT = Instant.parse("2026-11-22T09:45:01.234Z");

    @Test
    @DisplayName("定稿口径：sha256(vin|bindingOperationId|sourceFactRef|occurredAt)，Hex 小写 64 位")
    void compute_usesFinalDefinition() {
        String digest = ActivationPayloadDigest.compute(VIN, OP, REF, OCCURRED_AT);
        assertEquals(ActivationPayloadDigest.DIGEST_LENGTH, digest.length(), "摘要应为 sha256 Hex 长度");
        assertEquals(digest.toLowerCase(), digest, "摘要应为小写 Hex");
        // 与 eventId 无关：同一算法对同一业务事实稳定复算
        assertEquals(digest, ActivationPayloadDigest.compute(VIN, OP, REF, OCCURRED_AT));
    }

    @Test
    @DisplayName("TC-077-18：摘要与 eventId 无关，sourceFactRef 为空按空串参与")
    void compute_ignoresEventIdAndTreatsNullRefAsEmpty() {
        String withNullRef = ActivationPayloadDigest.compute(VIN, OP, null, OCCURRED_AT);
        String withEmptyRef = ActivationPayloadDigest.compute(VIN, OP, "", OCCURRED_AT);
        assertEquals(withEmptyRef, withNullRef, "null sourceFactRef 必须等同空串参与，不得写 null 字面量");
        assertNotEquals(ActivationPayloadDigest.compute(VIN, OP, REF, OCCURRED_AT), withNullRef,
                "sourceFactRef 非空时应改变摘要");
        // 不同 vin / 不同 occurredAt 应改变摘要（摘要覆盖全部业务字段）
        assertNotEquals(withNullRef, ActivationPayloadDigest.compute("LSVAA1234567890ZZ", OP, null, OCCURRED_AT));
        assertNotEquals(withNullRef, ActivationPayloadDigest.compute(VIN, OP, null,
                Instant.parse("2026-11-22T09:45:02.000Z")));
        // 摘要不参与 eventId：这里不存在 eventId 入参，结构上即与 eventId 无关
        assertFalse(withNullRef.contains("UVA"));
    }

    @Test
    @DisplayName("matches：大小写与空白容忍，空值一律不匹配")
    void matches_isLenientOnCaseButNeverOnEmpty() {
        String digest = ActivationPayloadDigest.compute(VIN, OP, REF, OCCURRED_AT);
        assertTrue(ActivationPayloadDigest.matches(digest, digest.toUpperCase()));
        assertTrue(ActivationPayloadDigest.matches(digest, " " + digest + " "));
        assertFalse(ActivationPayloadDigest.matches(digest, null));
        assertFalse(ActivationPayloadDigest.matches(digest, ""));
        assertFalse(ActivationPayloadDigest.matches(null, digest));
        assertFalse(ActivationPayloadDigest.matches(digest, patch(digest)));
    }

    @Test
    @DisplayName("旧口径 sha256(eventId|vin|bindingOperationId) 不再产出相同摘要（口径已切换）")
    void legacyDigestDiffers() {
        String finalDigest = ActivationPayloadDigest.compute(VIN, OP, null, OCCURRED_AT);
        String legacy = cn.hutool.crypto.digest.DigestUtil.sha256Hex(
                "UVA20261122094501123456|" + VIN + "|" + OP);
        assertNotEquals(legacy, finalDigest);
    }

    @Test
    @DisplayName("canonicalOccurredAtText：ISO-8601 + 毫秒向下截断（毫秒位为 0 时不输出小数）")
    void canonicalOccurredAtText_truncatesToMillis() {
        assertEquals("2026-11-22T09:45:01.234Z",
                ActivationPayloadDigest.canonicalOccurredAtText(Instant.parse("2026-11-22T09:45:01.234567891Z")),
                "纳秒部分必须向下截断到毫秒，不四舍五入、不提升精度");
        assertEquals("2026-11-22T09:45:01Z",
                ActivationPayloadDigest.canonicalOccurredAtText(Instant.parse("2026-11-22T09:45:01.000Z")),
                "毫秒位为 0 时 Instant.toString 省略小数（口径写死）");
        assertEquals("2026-11-22T09:45:01.999Z",
                ActivationPayloadDigest.canonicalOccurredAtText(Instant.parse("2026-11-22T09:45:01.999999Z")),
                "截断不得进位到下一秒");
        assertEquals("", ActivationPayloadDigest.canonicalOccurredAtText(null), "空时刻按空串参与");
        // 毫秒精度输入截断后不变（幂等）
        assertEquals("2026-11-22T09:45:01.234Z", ActivationPayloadDigest.canonicalOccurredAtText(OCCURRED_AT));
    }

    @Test
    @DisplayName("verify ①原文口径：事件字符串原文参与哈希即一致（不解析再序列化）")
    void verify_rawTextWins() {
        // 带偏移量原文：解析成 Instant 后再序列化会变成 Z 形式，口径不一致会误报
        String rawText = "2026-11-22T17:45:01.234+08:00";
        String digestOverRawText = ActivationPayloadDigest.compute(VIN, OP, REF, rawText);
        assertTrue(ActivationPayloadDigest.verify(digestOverRawText, VIN, OP, REF, rawText,
                Instant.parse(rawText)), "原文口径必须命中");
        // 无小数位的原文同理
        String noFraction = "2026-11-22T09:45:01Z";
        assertTrue(ActivationPayloadDigest.verify(ActivationPayloadDigest.compute(VIN, OP, REF, noFraction),
                VIN, OP, REF, noFraction, Instant.parse(noFraction)));
    }

    @Test
    @DisplayName("verify ②毫秒口径：生产端按毫秒截断 Instant.toString 计算摘要即一致")
    void verify_canonicalMillisWins() {
        String rawText = "2026-11-22T09:45:01.234Z";
        String digest = ActivationPayloadDigest.compute(VIN, OP, REF, OCCURRED_AT);
        assertTrue(ActivationPayloadDigest.verify(digest, VIN, OP, REF, rawText, OCCURRED_AT));
        // 原文缺失（隔离归档占位路径）时仍可校验
        assertTrue(ActivationPayloadDigest.verify(digest, VIN, OP, REF, null, OCCURRED_AT));
    }

    @Test
    @DisplayName("verify ③亚毫秒窗口：生产端按 datetime(3) 落库前的微秒精度哈希不再误报")
    void verify_subMilliWindowToleratesRepositoryRounding() {
        // 生产端事实：digest 按 2026-11-23T11:40:49.835740Z 计算，但 occurredAt 落库（datetime(3)）后发布为 .836Z
        Instant occurredAt = Instant.parse("2026-11-23T11:40:49.836Z");
        String rawText = "2026-11-23T11:40:49.836Z";
        String producerDigest = ActivationPayloadDigest.compute(VIN, OP, REF, "2026-11-23T11:40:49.835740Z");
        assertNotEquals(producerDigest, ActivationPayloadDigest.compute(VIN, OP, REF, rawText),
                "前置：两者按原文口径本不相等（复现误报根因）");
        assertTrue(ActivationPayloadDigest.verify(producerDigest, VIN, OP, REF, rawText, occurredAt),
                "亚毫秒差异属等价类，校验应通过");
        // 超出窗口（≥1ms 偏移）仍判不一致
        String outOfWindow = ActivationPayloadDigest.compute(VIN, OP, REF, "2026-11-23T11:40:49.834Z");
        assertFalse(ActivationPayloadDigest.verify(outOfWindow, VIN, OP, REF, rawText, occurredAt),
                "窗口外（1ms 及以上）差异必须仍判不一致");
    }

    @Test
    @DisplayName("verify 负例：vin/操作ID/来源引用/时刻任一不同 → 不一致（窗口不掩盖真实差异）")
    void verify_rejectsRealMismatch() {
        String rawText = "2026-11-22T09:45:01.234Z";
        assertFalse(ActivationPayloadDigest.verify(
                ActivationPayloadDigest.compute("LSVAA1234567890ZZ", OP, REF, OCCURRED_AT),
                VIN, OP, REF, rawText, OCCURRED_AT), "VIN 不同必须不一致");
        assertFalse(ActivationPayloadDigest.verify(
                ActivationPayloadDigest.compute(VIN, OP, "vso:order:OTHER", OCCURRED_AT),
                VIN, OP, REF, rawText, OCCURRED_AT), "sourceFactRef 不同必须不一致");
        assertFalse(ActivationPayloadDigest.verify(
                ActivationPayloadDigest.compute(VIN, OP, REF, "2026-11-22T09:45:02.234Z"),
                VIN, OP, REF, rawText, OCCURRED_AT), "时刻差 1s 必须不一致");
        assertFalse(ActivationPayloadDigest.verify(null, VIN, OP, REF, rawText, OCCURRED_AT), "空摘要不算校验通过");
        assertFalse(ActivationPayloadDigest.verify("  ", VIN, OP, REF, rawText, OCCURRED_AT), "空白摘要不算校验通过");
        assertFalse(ActivationPayloadDigest.verify(patch(ActivationPayloadDigest.compute(VIN, OP, REF, OCCURRED_AT)),
                VIN, OP, REF, rawText, OCCURRED_AT), "篡改摘要必须不一致");
    }

    /**
     * 修改摘要最后一位
     */
    private String patch(String digest) {
        char last = digest.charAt(digest.length() - 1);
        return digest.substring(0, digest.length() - 1) + (last == 'a' ? 'b' : 'a');
    }

}
