package net.hwyz.iov.cloud.edd.vmd.service.timeit;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CR-073 G1 Hutool Outbox writer 行为固化（TC-TIME-09 观察，不改 writer）。
 * <p>
 * 用户确认第 6 点：生产 writer/转换器/调用方保持不动，本测试只固化行为并记录 JVM 时区相关证据。
 * 实测（CR-073 报告）：hutool JSONObject 将 LocalDateTime 序列化为 epoch 毫秒（按 JVM 默认时区换算），
 * 而 {@code JSONUtil.toJsonStr(ldt)}（裸值）输出对象形式；Instant/Date 输出空对象。
 */
@DisplayName("CR-073 G1 Hutool 时间序列化 golden（不改 writer）")
class HutoolTimeGoldenIT {

    @Test
    @DisplayName("hutool JSONObject: LocalDateTime→epoch 毫秒按 JVM 默认时区换算（固化）")
    void jsonObject_ldt_toEpochMilli_jvmTzDependent() throws IOException {
        LocalDateTime ldt = LocalDateTime.of(2026, 10, 30, 14, 33, 59, 123_000_000);
        JSONObject o = new JSONObject();
        o.set("occurredAt", ldt);
        String json = o.toString();
        long epoch = Long.parseLong(json.replaceAll("\\D", "")); // 仅数字部分
        long expected = ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        assertEquals(expected, epoch,
                "hutool JSONObject 对 LocalDateTime 的 epoch 输出必须等于按 JVM 默认时区换算（固化行为）");
        // 固化 JVM 时区上下文：输出值随 JVM TZ 漂移是既有 writer 行为，本批不修复
        System.out.println("[CR073-G1][记录] hutool JSONObject LDT→epoch: " + epoch
                + " (JVM_TZ=" + TimeItSchema.jvmTz() + ", LDT=" + ldt + ")");
        appendEvidence("hutool-jsonobject-ldt: " + json + " | jvmTz=" + TimeItSchema.jvmTz() + " | expected=" + expected);
    }

    @Test
    @DisplayName("hutool 裸值 JSONUtil.toJsonStr(LDT)=对象形式；Instant/Date=空对象（固化）")
    void jsonUtil_bareValues_shapes() {
        LocalDateTime ldt = LocalDateTime.of(2026, 10, 30, 14, 33, 59);
        Instant inst = Instant.parse("2026-10-30T06:33:59Z");
        String ldtJson = JSONUtil.toJsonStr(ldt);
        String instJson = JSONUtil.toJsonStr(inst);
        assertTrue(ldtJson.startsWith("{\"year\""), "裸 LDT 输出对象形式（既有 writer 行为）: " + ldtJson);
        assertEquals("{}", instJson, "裸 Instant 输出空对象（既有 writer 行为）");
        // JSONObject 容器内（生产 Outbox 实际路径）
        JSONObject container = new JSONObject();
        container.set("instant", inst);
        assertEquals("2026-10-30T06:33:59Z", container.get("instant").toString(),
                "JSONObject 内 Instant 字段序列化为 ISO 字符串（生产 Outbox 实际 writer 行为，冻结）");
    }

    @Test
    @DisplayName("hutool epoch 输出与绝对时刻对账（Outbox 真实样本语义）")
    void hutool_epoch_vs_absolute() throws IOException {
        // 模拟 VehicleProduceEventEnvelope 写路径（生产 Outbox 实际 writer）
        LocalDateTime occurredAt = LocalDateTime.of(2026, 7, 18, 15, 30, 12); // 与 dev 库样本形态一致（上海墙钟）
        JSONObject envelope = new JSONObject();
        envelope.set("occurredAt", occurredAt);
        long epoch = Long.parseLong(envelope.toString().replaceAll("\\D", ""));
        Instant asAbsolute = Instant.ofEpochMilli(epoch);
        System.out.println("[CR073-G1][记录] Outbox writer 语义: LDT=" + occurredAt + " → epoch=" + epoch
                + " → Instant=" + asAbsolute + "（JVM_TZ=" + TimeItSchema.jvmTz() + "，UTC 解释偏差="
                + (occurredAt.toInstant(ZoneId.systemDefault().getRules().getOffset(occurredAt))
                        .toEpochMilli() - occurredAt.toInstant(java.time.ZoneOffset.UTC).toEpochMilli()) / 3_600_000 + "h）");
        appendEvidence("hutool-outbox-writer: LDT=" + occurredAt + " epoch=" + epoch
                + " instantIfUtc=" + asAbsolute + " jvmTz=" + TimeItSchema.jvmTz());
    }

    private static void appendEvidence(String line) throws IOException {
        Path dir = Paths.get(System.getProperty("time.it.outdir", "target/time-governance"));
        Files.createDirectories(dir);
        Path out = dir.resolve("hutool-golden-" + TimeItSchema.jvmTz().replace("/", "_") + ".log");
        Files.writeString(out, line + "\n", StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
    }
}
