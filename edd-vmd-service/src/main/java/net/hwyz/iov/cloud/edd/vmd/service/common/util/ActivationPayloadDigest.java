package net.hwyz.iov.cloud.edd.vmd.service.common.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 激活事实载荷摘要（VMD-DSN-CR-077 / D-VMD-077-05，R-10 定稿口径 + 终审修复 B-4 残留）
 * <p>
 * 定稿口径：{@code sha256(vin | bindingOperationId | sourceFactRef | occurredAtText)}，Hex 小写。
 * <ul>
 *   <li><strong>与 {@code eventId} 无关</strong>：同一次业务事实因重投产生不同 {@code eventId} 时，
 *       摘要必须保持一致，因此摘要差异<strong>不构成</strong>冲突终态判定依据</li>
 *   <li>{@code sourceFactRef} 为空时按空串参与（不写 {@code null} 字面量）</li>
 *   <li>旧口径 {@code sha256(eventId|vin|bindingOperationId)} 已废弃，本类不实现、不依赖</li>
 * </ul>
 * <h3>{@code occurredAtText} 口径（两侧必须一致，终审修复 B-4 残留）</h3>
 * <ol>
 *   <li><strong>原文优先</strong>：事件已携带 {@code payloadDigest} 时，VMD 复算一律使用事件
 *       {@code occurredAt} 字段的<strong>字符串原文</strong>参与哈希（不做解析再序列化），
 *       从而与生产端「按 payload 字符串哈希」的形态逐字对齐；</li>
 *   <li><strong>规范化口径（VMD 落库/缺省回算）</strong>：无法取得原文（如隔离归档的占位路径）时，
 *       取 ISO-8601 Instant 字符串并<strong>按毫秒截断</strong>
 *       （{@link ChronoUnit#MILLIS} 向下取整，与 VMD/TSP 两侧 {@code datetime(3)} 落库精度一致），
 *       序列化沿用 {@link Instant#toString()} 的小数位分组规则：
 *       毫秒位为 0 时不输出小数（{@code ...:01Z}），否则输出 3 位（{@code ...:01.234Z}）；
 *       微秒/纳秒精度仅在原文自带时保留（6/9 位），VMD 不主动提升精度。</li>
 * </ol>
 * 摘要仅用于观测与数据质量核对，不改变任何幂等/终态判定。
 *
 * @author hwyz_leo
 */
public final class ActivationPayloadDigest {

    private static final String SEPARATOR = "|";

    /**
     * 摘要 Hex 长度（sha256）
     */
    public static final int DIGEST_LENGTH = 64;

    /**
     * 亚毫秒兼容窗口（微秒）：生产端把 {@code occurredAt} 以 {@code datetime(3)} 落库（毫秒），
     * 但若其摘要在落库前按微秒精度 Instant 计算，则事件内 {@code occurredAt} 与其摘要输入
     * 相差不超过 ±500µs（四舍五入到毫秒）。校验时在该窗口内枚举微秒候选，仅用于完整性校验。
     */
    private static final int SUB_MILLI_WINDOW_MICROS = 500;

    private ActivationPayloadDigest() {
    }

    /**
     * 按定稿口径回算载荷摘要（{@code occurredAtText} 已给定原文）
     *
     * @param vin                车架号（已规范化）
     * @param bindingOperationId 绑定操作 ID
     * @param sourceFactRef      来源事实引用（可空，空按空串参与）
     * @param occurredAtText     事实发生时刻字符串原文（参与哈希的原始文本）
     * @return sha256 Hex 小写摘要
     */
    public static String compute(String vin, String bindingOperationId, String sourceFactRef, String occurredAtText) {
        String payload = StrUtil.nullToEmpty(vin)
                + SEPARATOR + StrUtil.nullToEmpty(bindingOperationId)
                + SEPARATOR + StrUtil.nullToEmpty(sourceFactRef)
                + SEPARATOR + StrUtil.nullToEmpty(occurredAtText);
        return DigestUtil.sha256Hex(payload);
    }

    /**
     * 按定稿口径回算载荷摘要（无原文时的规范化口径：毫秒截断 ISO-8601）
     *
     * @param vin                车架号（已规范化）
     * @param bindingOperationId 绑定操作 ID
     * @param sourceFactRef      来源事实引用（可空，空按空串参与）
     * @param occurredAt         事实发生时刻（UTC），按 {@link #canonicalOccurredAtText(Instant)} 序列化
     * @return sha256 Hex 小写摘要
     */
    public static String compute(String vin, String bindingOperationId, String sourceFactRef, Instant occurredAt) {
        return compute(vin, bindingOperationId, sourceFactRef, canonicalOccurredAtText(occurredAt));
    }

    /**
     * 规范化 {@code occurredAt} 摘要文本：ISO-8601（{@link Instant#toString()}）+ <strong>毫秒截断</strong>
     * <p>
     * 截断规则（与两侧 {@code datetime(3)} 落库精度一致，写死为口径）：
     * 纳秒部分向下取整到毫秒（{@code nano - nano % 1_000_000}），不四舍五入，不提升精度；
     * 毫秒位为 0 时 {@link Instant#toString()} 自然省略小数部分。
     *
     * @param occurredAt 时刻（可空）
     * @return 摘要文本（可空入参返回空串，与 {@code null} 按空串参与哈希一致）
     */
    public static String canonicalOccurredAtText(Instant occurredAt) {
        if (occurredAt == null) {
            return "";
        }
        return occurredAt.truncatedTo(ChronoUnit.MILLIS).toString();
    }

    /**
     * 安全比较：任一为空 / 格式差异（大小写、空白）均视为不一致
     *
     * @param expected 期望摘要
     * @param actual   事件提供的摘要（可空）
     * @return true=一致
     */
    public static boolean matches(String expected, String actual) {
        if (StrUtil.isBlank(expected) || StrUtil.isBlank(actual)) {
            return false;
        }
        return expected.equalsIgnoreCase(actual.trim());
    }

    /**
     * 完整性校验：事件提供的摘要是否与本地复算等价（<strong>仅校验，不参与落库与判定</strong>）
     * <p>
     * 候选口径（按优先级，命中即一致）：
     * <ol>
     *   <li>事件 {@code occurredAt} <strong>字符串原文</strong>（生产端按 payload 原文哈希）</li>
     *   <li>规范化为毫秒截断的 ISO-8601（生产端按 {@code Instant.toString()} / {@code datetime(3)} 哈希）</li>
     *   <li>亚毫秒兼容窗口：毫秒值 ±500µs 内枚举微秒候选（生产端摘要在 {@code datetime(3)} 落库前
     *       按微秒精度计算导致的等价类；<strong>只有</strong>时刻亚毫秒位不同才会命中，
     *       {@code vin}/{@code bindingOperationId}/{@code sourceFactRef}/毫秒时刻任一不同仍判不一致）</li>
     * </ol>
     *
         * @param providedDigest 事件提供的摘要（可空）
     * @param vin 车架号（已规范化）
     * @param bindingOperationId 绑定操作 ID
     * @param sourceFactRef 来源事实引用（可空）
     * @param occurredAtText 事件 {@code occurredAt} 原文（可空）
     * @param occurredAt 解析后的事实时刻（可空）
     * @return true=校验通过（摘要等价）
     */
    public static boolean verify(String providedDigest, String vin, String bindingOperationId, String sourceFactRef,
                                 String occurredAtText, Instant occurredAt) {
        if (StrUtil.isBlank(providedDigest)) {
            return false;
        }
        // ① 原文口径
        if (StrUtil.isNotBlank(occurredAtText)
                && matches(compute(vin, bindingOperationId, sourceFactRef, occurredAtText), providedDigest)) {
            return true;
        }
        if (occurredAt == null) {
            return false;
        }
        // ② 规范化毫秒口径
        if (matches(compute(vin, bindingOperationId, sourceFactRef, occurredAt), providedDigest)) {
            return true;
        }
        // ③ 亚毫秒兼容窗口
        return matchesSubMilliWindow(providedDigest, vin, bindingOperationId, sourceFactRef, occurredAt);
    }

    /**
     * 亚毫秒兼容窗口校验（±500µs，毫秒截断为基准）
     *
     * @param providedDigest     事件提供的摘要
     * @param vin                车架号
     * @param bindingOperationId 绑定操作 ID
     * @param sourceFactRef      来源事实引用
     * @param occurredAt         毫秒精度的分界时刻
     * @return true=窗口内存在等价摘要
     */
    private static boolean matchesSubMilliWindow(String providedDigest, String vin, String bindingOperationId,
                                                 String sourceFactRef, Instant occurredAt) {
        long milliEpoch = occurredAt.truncatedTo(ChronoUnit.MILLIS).toEpochMilli();
        for (int micros = -SUB_MILLI_WINDOW_MICROS; micros < SUB_MILLI_WINDOW_MICROS; micros++) {
            if (micros == 0) {
                continue;
            }
            String text = Instant.ofEpochMilli(milliEpoch).plusNanos(micros * 1_000L).toString();
            if (matches(compute(vin, bindingOperationId, sourceFactRef, text), providedDigest)) {
                return true;
            }
        }
        return false;
    }

}
