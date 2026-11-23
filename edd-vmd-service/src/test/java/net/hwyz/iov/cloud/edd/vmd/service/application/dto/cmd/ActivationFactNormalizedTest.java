package net.hwyz.iov.cloud.edd.vmd.service.application.dto.cmd;

import net.hwyz.iov.cloud.edd.vmd.service.application.dto.event.UserVehicleActivationFactEvent;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.ActivationFactContractException;
import net.hwyz.iov.cloud.edd.vmd.service.common.util.ActivationPayloadDigest;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.ActivationDigestSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 激活事实契约校验与规范化单元测试（VMD-DSN-CR-077 §4.1 / TC-077-08 / TC-077-20 / 终审修复 B-4 残留）
 * <p>
 * 摘要来源口径（定稿）：
 * ① 事件带摘要且与本地复算一致 → {@code EVENT}；
 * ② 事件带摘要但与本地复算不一致 → <strong>仍 {@code EVENT}（事件值为权威落库）</strong> + 校验不一致标记；
 * ③ 事件不带摘要 → 本地复算 {@code LOCAL}。
 *
 * @author hwyz_leo
 */
@DisplayName("ActivationFactNormalized 契约校验测试")
class ActivationFactNormalizedTest {

    private static final String VIN = "LSVAA1234567890AB";
    private static final String OP = "BOP20261122094501000123";
    private static final String REF = "vso:order:SO20261120001#delivery";
    private static final String OCCURRED_AT_TEXT = "2026-11-22T09:45:01.234Z";

    private UserVehicleActivationFactEvent.UserVehicleActivationFactEventBuilder base() {
        return UserVehicleActivationFactEvent.builder()
                .eventId("UVA20261122094501123456")
                .schemaVersion(1)
                .vin(VIN)
                .bindingOperationId(OP)
                .sourceFactRef(REF)
                .occurredAt(OCCURRED_AT_TEXT);
    }

    @Test
    @DisplayName("合法载荷：VIN 规范化 + occurredAt 解析 + 摘要本地回算（无事件摘要 → LOCAL）")
    void validEvent_normalizes() {
        UserVehicleActivationFactEvent event = base().vin(" " + VIN.toLowerCase() + " ").build();
        ActivationFactNormalized fact = ActivationFactNormalized.of(event, VIN);
        assertEquals(VIN, fact.vin(), "VIN 应 trim + 大写规范化");
        assertEquals(Instant.parse(OCCURRED_AT_TEXT), fact.occurredAt());
        assertEquals(ActivationDigestSource.LOCAL, fact.digestSource(), "生产端未输出摘要时应为 LOCAL");
        assertFalse(fact.digestProvided());
        assertFalse(fact.digestProvidedButMismatch());
        assertEquals(ActivationPayloadDigest.DIGEST_LENGTH, fact.payloadDigest().length());
        assertEquals(fact.localDigest(), fact.payloadDigest(), "无事件摘要时落库值即本地回算值");
        assertEquals(OCCURRED_AT_TEXT, fact.occurredAtText(), "原文须保留，参与摘要哈希");
    }

    @Test
    @DisplayName("①事件带摘要且与本地一致 → digest_source=EVENT")
    void providedDigestMatches_sourceEvent() {
        String digest = ActivationPayloadDigest.compute(VIN, OP, REF, OCCURRED_AT_TEXT);
        ActivationFactNormalized fact = ActivationFactNormalized.of(
                base().payloadDigest(digest).build(), VIN);
        assertEquals(ActivationDigestSource.EVENT, fact.digestSource());
        assertTrue(fact.digestProvided());
        assertFalse(fact.digestProvidedButMismatch());
        assertEquals(digest, fact.payloadDigest(), "事件摘要应作为落库权威值");
    }

    @Test
    @DisplayName("②事件带摘要但与本地不一致 → 仍 EVENT（事件值权威）+ 校验不一致标记，不降级 LOCAL")
    void providedDigestMismatch_stillEvent() {
        String provided = "0".repeat(ActivationPayloadDigest.DIGEST_LENGTH);
        ActivationFactNormalized fact = ActivationFactNormalized.of(
                base().payloadDigest(provided).build(), VIN);
        assertEquals(ActivationDigestSource.EVENT, fact.digestSource(),
                "生产端摘要为权威：不一致也不得降级为 LOCAL");
        assertEquals(provided, fact.payloadDigest(), "落库值必须是事件摘要（与 db_tsp 对齐）");
        assertTrue(fact.digestProvidedButMismatch(), "不一致仅记观测标记，不影响判定");
        assertNotEquals(fact.payloadDigest(), fact.localDigest(), "本地复算值仅用于校验/观测");
    }

    @Test
    @DisplayName("②'生产端按微秒精度哈希（datetime(3) 落库前）→ 校验通过，不产生误报")
    void providedDigestMicroPrecision_verifiesWithoutMismatch() {
        // 复现实测：digest 按 11:40:49.835740Z 计算，occurredAt 落库后发布为 .836Z
        String producerDigest = ActivationPayloadDigest.compute(VIN, OP, REF, "2026-11-23T11:40:49.835740Z");
        ActivationFactNormalized fact = ActivationFactNormalized.of(
                base().occurredAt("2026-11-23T11:40:49.836Z").payloadDigest(producerDigest).build(), VIN);
        assertEquals(ActivationDigestSource.EVENT, fact.digestSource());
        assertFalse(fact.digestProvidedButMismatch(), "亚毫秒窗口内应判校验通过（消除误报）");
    }

    @Test
    @DisplayName("②''原文口径：带偏移量 occurredAt 按原文参与哈希，校验通过")
    void providedDigestOverRawOffsetText_verifies() {
        String rawText = "2026-11-22T17:45:01.234+08:00";
        String digestOverRawText = ActivationPayloadDigest.compute(VIN, OP, REF, rawText);
        ActivationFactNormalized fact = ActivationFactNormalized.of(
                base().occurredAt(rawText).payloadDigest(digestOverRawText).build(), VIN);
        assertEquals(Instant.parse(OCCURRED_AT_TEXT), fact.occurredAt(), "解析后归一到 UTC");
        assertEquals(rawText, fact.occurredAtText(), "原文保留");
        assertFalse(fact.digestProvidedButMismatch(), "原文口径必须命中");
    }

    @Test
    @DisplayName("摘要超长（>64）→ 契约错误（隔离，不写坏台账列）")
    void payloadDigestTooLong_quarantined() {
        assertThrows(ActivationFactContractException.class,
                () -> ActivationFactNormalized.of(base().payloadDigest("a".repeat(65)).build(), null));
    }

    @Test
    @DisplayName("TC-077-08：缺必填 / schemaVersion=2 / 时间不可解析 / VIN 超长 → 契约错误（隔离）")
    void contractErrors_throwForQuarantine() {
        assertThrows(ActivationFactContractException.class,
                () -> ActivationFactNormalized.of(base().vin(null).build(), null), "缺 vin 应隔离");
        assertThrows(ActivationFactContractException.class,
                () -> ActivationFactNormalized.of(base().eventId(" ").build(), null), "缺 eventId 应隔离");
        assertThrows(ActivationFactContractException.class,
                () -> ActivationFactNormalized.of(base().bindingOperationId(null).build(), null),
                "缺 bindingOperationId 应隔离");
        assertThrows(ActivationFactContractException.class,
                () -> ActivationFactNormalized.of(base().schemaVersion(2).build(), null), "版本不符应隔离");
        assertThrows(ActivationFactContractException.class,
                () -> ActivationFactNormalized.of(base().schemaVersion(null).build(), null), "缺版本应隔离");
        assertThrows(ActivationFactContractException.class,
                () -> ActivationFactNormalized.of(base().occurredAt("not-a-time").build(), null),
                "时间不可解析应隔离");
        assertThrows(ActivationFactContractException.class,
                () -> ActivationFactNormalized.of(base().occurredAt(null).build(), null), "缺时间应隔离");
        assertThrows(ActivationFactContractException.class,
                () -> ActivationFactNormalized.of(base().sourceFactRef("x".repeat(513)).build(), null),
                "sourceFactRef 超长应隔离");
    }

    @Test
    @DisplayName("TC-077-20：21 位 VIN 隔离（不截断），20 位合法 VIN 正常")
    void vinWidth_21Rejected20Accepted() {
        assertThrows(ActivationFactContractException.class,
                () -> ActivationFactNormalized.of(base().vin("LSVAA1234567890ABCDEF").build(), null),
                "21 位 VIN 必须隔离，不得截断");
        ActivationFactNormalized fact = ActivationFactNormalized.of(
                base().vin("LSVAA1234567890ABCDE").build(), null);
        assertEquals(20, fact.vin().length());
    }

    @Test
    @DisplayName("Kafka Key 与 payload.vin 不一致 → 契约错误（隔离）")
    void kafkaKeyMismatch_quarantined() {
        assertThrows(ActivationFactContractException.class,
                () -> ActivationFactNormalized.of(base().build(), "LSVAA1234567890ZZ"));
    }

    @Test
    @DisplayName("带偏移量的 occurredAt 可解析并归一到 UTC")
    void offsetDateTimeAccepted() {
        ActivationFactNormalized fact = ActivationFactNormalized.of(
                base().occurredAt("2026-11-22T17:45:01.234+08:00").build(), null);
        assertEquals(Instant.parse(OCCURRED_AT_TEXT), fact.occurredAt());
    }

}
