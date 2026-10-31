package net.hwyz.iov.cloud.edd.vmd.service.timeit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * CR-073 TC-TIME-02/03 隔离 JDBC 矩阵（JUnit 执行器；脚本负责双 JVM 交叉）。
 * <p>
 * 分组断言（用户确认第 1 点）：
 * <ul>
 *   <li>候选 UTC 路径（UTC_CANDIDATE）= 目标验收组：Instant 往返绝对一致、精度按 (0)/(3)/(6) 声明验收；</li>
 *   <li>默认 URL/旧 handler = 风险复现组：在「Asia/Shanghai JVM + 非亚洲服务器会话」条件成立时断言 +8h 偏移可复现，
 *       条件不成立则记录不失败（不得为全绿放宽样本/断言）；</li>
 *   <li>负 epoch / 超 TIMESTAMP 范围样本验证拒绝或边界行为。</li>
 * </ul>
 * 证据记录（用户确认第 2 点）：JVM TZ、写连接与核验连接 @@session.time_zone、列类型/精度、
 * JDBC setter/getter、固定 UTC 核验连接下 getString 展示值与还原 Instant。
 */
@DisplayName("CR-073 G1 JDBC 时区/精度矩阵（隔离库）")
@EnabledIfSystemProperty(named = "time.it.mysql.host", matches = ".*", disabledReason = "仅显式指定 MySQL 时执行")
class TimeJdbcMatrixIT {

    private static String schema;
    private static final List<TimeMatrixEngine.Row> ROWS = new ArrayList<>();

    @BeforeAll
    static void init() throws Exception {
        assumeTrue(TimeItSchema.mysqlReachable(), "MySQL 不可达：本 IT 未执行（不得验收通过）");
        schema = TimeItSchema.newSchemaName();
        try (Connection admin = TimeItSchema.open(TimeItSchema.jdbcServer())) {
            TimeItSchema.createSchema(admin, schema);
        }
        try (Connection c = TimeItSchema.open(TimeItSchema.defaultUrl(schema))) {
            TimeItSchema.createMatrixTable(c, schema);
        }
    }

    @AfterAll
    static void cleanup() throws Exception {
        if (schema != null) {
            try (Connection admin = TimeItSchema.open(TimeItSchema.jdbcServer())) {
                TimeItSchema.dropSchema(admin, schema);
            }
        }
        persistRows();
    }

    private static void persistRows() {
        try {
            Path dir = Paths.get(System.getProperty("time.it.outdir", "target/time-governance"));
            Files.createDirectories(dir);
            Path out = dir.resolve("matrix-singlejvm-" + TimeItSchema.jvmTz().replace("/", "_") + ".json");
            StringBuilder sb = new StringBuilder();
            for (TimeMatrixEngine.Row r : ROWS) {
                sb.append(r.toJsonLine()).append('\n');
            }
            Files.writeString(out, sb.toString(), StandardCharsets.UTF_8);
            System.out.println("[CR073-G1] 矩阵证据写入: " + out.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("[CR073-G1] 证据写入失败: " + e.getMessage());
        }
    }

    // ---------- 候选 UTC 验收组 ----------

    @Test
    @DisplayName("候选UTC：Instant→(6) 绝对时刻逐位一致（微秒），三种 handler 语义")
    void candidateUtc_instantMicro_roundTrip() {
        TimeSample s3 = Stream.of(TimeSample.all()).filter(s -> s.name().equals("S3_MICRO")).findFirst().orElseThrow();
        for (TimeMatrixEngine.Writer w : new TimeMatrixEngine.Writer[]{
                TimeMatrixEngine.Writer.INSTANT_SET_TIMESTAMP,
                TimeMatrixEngine.Writer.UTC_WALL_SET_OBJECT,
                TimeMatrixEngine.Writer.DATE_SET_TIMESTAMP}) {
            for (TimeMatrixEngine.Col col : new TimeMatrixEngine.Col[]{
                    TimeMatrixEngine.Col.DT6, TimeMatrixEngine.Col.TS6}) {
                TimeMatrixEngine.Row r = TimeMatrixEngine.writeAndVerify(schema, TimeMatrixEngine.ConnMode.UTC_CANDIDATE, w, col, s3);
                ROWS.add(r);
                assertEquals("OK", r.status(), "写入应成功: " + r);
                // Instant/LDT(UTC墙) 路径保留微秒逐位一致；java.util.Date 仅毫秒精度（Date 承载精度证据）
                java.time.Instant expected = w == TimeMatrixEngine.Writer.DATE_SET_TIMESTAMP
                        ? s3.instant().truncatedTo(java.time.temporal.ChronoUnit.MILLIS)
                        : s3.instant();
                assertEquals(expected, Instant.parse(r.readInstant()),
                        "候选UTC绝对时刻必须按 writer 承载精度一致: " + r);
            }
        }
    }

    @Test
    @DisplayName("候选UTC：精度按列声明验收（(6)微秒、(3)毫秒、(0)秒级截断为预期精度损失）")
    void candidateUtc_precisionByDeclaration() {
        TimeSample s2 = Stream.of(TimeSample.all()).filter(s -> s.name().equals("S2_MILLI")).findFirst().orElseThrow();
        TimeSample s3 = Stream.of(TimeSample.all()).filter(s -> s.name().equals("S3_MICRO")).findFirst().orElseThrow();
        // (3) 毫秒列：微秒样本截断到毫秒（声明精度损失，不得要求保留微秒）
        TimeMatrixEngine.Row r3 = TimeMatrixEngine.writeAndVerify(schema, TimeMatrixEngine.ConnMode.UTC_CANDIDATE,
                TimeMatrixEngine.Writer.INSTANT_SET_TIMESTAMP, TimeMatrixEngine.Col.DT3, s3);
        ROWS.add(r3);
        assertEquals("OK", r3.status(), r3.toJsonLine());
        assertEquals(s3.instant().truncatedTo(java.time.temporal.ChronoUnit.MILLIS), Instant.parse(r3.readInstant()),
                "(3) 应按毫秒声明精度验收: " + r3.toJsonLine());
        // (3) 毫秒列：毫秒样本完整保留
        TimeMatrixEngine.Row r2 = TimeMatrixEngine.writeAndVerify(schema, TimeMatrixEngine.ConnMode.UTC_CANDIDATE,
                TimeMatrixEngine.Writer.INSTANT_SET_TIMESTAMP, TimeMatrixEngine.Col.DT3, s2);
        ROWS.add(r2);
        assertEquals(s2.instant(), Instant.parse(r2.readInstant()), "(3) 毫秒样本应完整: " + r2.toJsonLine());
        // (0) 秒级列：截断到秒为预期
        for (TimeMatrixEngine.Col col : new TimeMatrixEngine.Col[]{TimeMatrixEngine.Col.DT, TimeMatrixEngine.Col.TS}) {
            TimeMatrixEngine.Row r0 = TimeMatrixEngine.writeAndVerify(schema, TimeMatrixEngine.ConnMode.UTC_CANDIDATE,
                    TimeMatrixEngine.Writer.INSTANT_SET_TIMESTAMP, col, s3);
            ROWS.add(r0);
            assertEquals("OK", r0.status(), r0.toJsonLine());
            assertEquals(s3.instant().truncatedTo(java.time.temporal.ChronoUnit.SECONDS), Instant.parse(r0.readInstant()),
                    "(0) 秒级列应按秒截断(预期精度损失): " + r0.toJsonLine());
        }
    }

    @Test
    @DisplayName("候选UTC：LocalDateTime 墙钟原样读写（无转换、不按 UTC 解释）")
    void candidateUtc_ldtWall_unchanged() {
        TimeSample w = new TimeSample("WALL", null, TimeSample.WALL, "无 offset 业务墙钟");
        // (6) 位列：墙钟含微秒原样
        for (TimeMatrixEngine.Col col : new TimeMatrixEngine.Col[]{
                TimeMatrixEngine.Col.DT6, TimeMatrixEngine.Col.TS6}) {
            TimeMatrixEngine.Row r = TimeMatrixEngine.writeAndVerify(schema, TimeMatrixEngine.ConnMode.UTC_CANDIDATE,
                    TimeMatrixEngine.Writer.LDT_SET_OBJECT, col, w);
            ROWS.add(r);
            assertEquals("OK", r.status(), r.toJsonLine());
            assertEquals(TimeSample.WALL, LocalDateTime.parse(r.readLdt()),
                    "LDT 墙钟必须原样（纯墙钟不注册 UTC 转换，CR-073 RD-TIME-003）: " + r.toJsonLine());
        }
        // (0) 位列：墙钟截断到秒为声明精度损失（MySQL 对小数秒按 HALF_UP 舍入到声明位）
        TimeMatrixEngine.Row r0 = TimeMatrixEngine.writeAndVerify(schema, TimeMatrixEngine.ConnMode.UTC_CANDIDATE,
                TimeMatrixEngine.Writer.LDT_SET_OBJECT, TimeMatrixEngine.Col.DT, w);
        ROWS.add(r0);
        assertEquals(LocalDateTime.of(2026, 10, 30, 8, 0, 0), LocalDateTime.parse(r0.readLdt()),
                "(0) 位列 LDT 按秒精度截断（预期精度损失）: " + r0.toJsonLine());
    }

    // ---------- 默认 URL / 旧 handler 风险复现组（条件断言） ----------

    @Test
    @DisplayName("默认URL旧handler：Asia/Shanghai JVM + 非亚洲服务器会话 下 +8h 偏移可复现；否则记录")
    void defaultLegacy_riskRepro_conditional() {
        TimeSample s3 = Stream.of(TimeSample.all()).filter(s -> s.name().equals("S3_MICRO")).findFirst().orElseThrow();
        boolean shJvm = TimeItSchema.jvmTz().equals("Asia/Shanghai");
        for (TimeMatrixEngine.Writer w : new TimeMatrixEngine.Writer[]{
                TimeMatrixEngine.Writer.INSTANT_SET_TIMESTAMP, TimeMatrixEngine.Writer.DATE_SET_TIMESTAMP}) {
            for (TimeMatrixEngine.Col col : new TimeMatrixEngine.Col[]{
                    TimeMatrixEngine.Col.DT6, TimeMatrixEngine.Col.TS6, TimeMatrixEngine.Col.DT}) {
                TimeMatrixEngine.Row r = TimeMatrixEngine.writeAndVerify(schema, TimeMatrixEngine.ConnMode.DEFAULT, w, col, s3);
                ROWS.add(r);
                if ("OK".equals(r.status()) && shJvm && !r.writeSessionTz().contains("+08:00")) {
                    // 复现条件：上海 JVM + 服务器会话非 +08:00（本地实测会话 SYSTEM/UTC）
                    Instant read = Instant.parse(r.readInstant());
                    Instant expectedShifted = shiftByColumn(s3.instant().plus(Duration.ofHours(8)), col);
                    if (w == TimeMatrixEngine.Writer.DATE_SET_TIMESTAMP) {
                        // java.util.Date 仅毫秒精度：偏移后微秒部分截断
                        expectedShifted = expectedShifted.truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
                    }
                    assertEquals(expectedShifted, read,
                            "风险复现：上海 JVM + 非亚洲会话 默认路径应复现 +8h 偏移（存储墙钟见 storedWallUtcCheck）: " + r.toJsonLine());
                } else {
                    System.out.println("[CR073-G1][记录] 默认路径无偏移断言条件（status=" + r.status() + ", jvm=" + TimeItSchema.jvmTz()
                            + ", writeSession=" + r.writeSessionTz() + "）: " + r.toJsonLine());
                }
            }
        }
    }

    // ---------- 范围/边界（TC-TIME-03：负 epoch、TIMESTAMP 上界、超上界） ----------

    @Test
    @DisplayName("范围边界：负 epoch / TIMESTAMP 上界 / 超上界 的拒绝或边界行为（候选UTC连接）")
    void rangeTimestamp_boundaryOrReject() {
        TimeSample s6 = Stream.of(TimeSample.all()).filter(s -> s.name().equals("S6_NEG_EPOCH")).findFirst().orElseThrow();
        TimeSample s7 = Stream.of(TimeSample.all()).filter(s -> s.name().equals("S7_TIMESTAMP_MAX")).findFirst().orElseThrow();
        TimeSample s8 = Stream.of(TimeSample.all()).filter(s -> s.name().equals("S8_BEYOND_TIMESTAMP_MAX")).findFirst().orElseThrow();
        TimeMatrixEngine.Writer w = TimeMatrixEngine.Writer.INSTANT_SET_TIMESTAMP;

        // S6 负 epoch：TIMESTAMP 列应拒绝或记录限制；DATETIME 应可成功存储并读回原值
        for (TimeMatrixEngine.Col col : new TimeMatrixEngine.Col[]{TimeMatrixEngine.Col.TS, TimeMatrixEngine.Col.TS6}) {
            TimeMatrixEngine.Row r = TimeMatrixEngine.writeAndVerify(schema, TimeMatrixEngine.ConnMode.UTC_CANDIDATE, w, col, s6);
            ROWS.add(r);
            assertTrue("REJECTED".equals(r.status()) || "null".equals(r.storedWallUtcCheck()),
                    "TIMESTAMP 不应成功保存 1970 下限之外的负 epoch（拒绝/范围限制，非要求成功）: " + r.toJsonLine());
        }
        for (TimeMatrixEngine.Col col : new TimeMatrixEngine.Col[]{TimeMatrixEngine.Col.DT, TimeMatrixEngine.Col.DT6}) {
            TimeMatrixEngine.Row r = TimeMatrixEngine.writeAndVerify(schema, TimeMatrixEngine.ConnMode.UTC_CANDIDATE, w, col, s6);
            ROWS.add(r);
            assertEquals("OK", r.status(), "DATETIME 范围内负 epoch 应可存: " + r.toJsonLine());
            assertEquals(shiftByColumn(s6.instant(), col), Instant.parse(r.readInstant()),
                    "DATETIME 负 epoch 按声明精度读回: " + r.toJsonLine());
        }

        // S7 TIMESTAMP 上界（含）应可存
        for (TimeMatrixEngine.Col col : new TimeMatrixEngine.Col[]{TimeMatrixEngine.Col.TS, TimeMatrixEngine.Col.TS6}) {
            TimeMatrixEngine.Row r = TimeMatrixEngine.writeAndVerify(schema, TimeMatrixEngine.ConnMode.UTC_CANDIDATE, w, col, s7);
            ROWS.add(r);
            assertEquals("OK", r.status(), "TIMESTAMP 上界应可存: " + r.toJsonLine());
            assertEquals(s7.instant(), Instant.parse(r.readInstant()), "TIMESTAMP 上界读回: " + r.toJsonLine());
        }

        // S8 超 TIMESTAMP 上界 1s：TIMESTAMP 拒绝/记录；DATETIME(6) 可存原值；DATETIME(0) 按秒舍入
        for (TimeMatrixEngine.Col col : new TimeMatrixEngine.Col[]{TimeMatrixEngine.Col.TS, TimeMatrixEngine.Col.TS6}) {
            TimeMatrixEngine.Row r = TimeMatrixEngine.writeAndVerify(schema, TimeMatrixEngine.ConnMode.UTC_CANDIDATE, w, col, s8);
            ROWS.add(r);
            assertTrue("REJECTED".equals(r.status()) || "null".equals(r.storedWallUtcCheck()),
                    "超 TIMESTAMP 上界应拒绝/范围限制: " + r.toJsonLine());
        }
        for (TimeMatrixEngine.Col col : new TimeMatrixEngine.Col[]{TimeMatrixEngine.Col.DT, TimeMatrixEngine.Col.DT6}) {
            TimeMatrixEngine.Row r = TimeMatrixEngine.writeAndVerify(schema, TimeMatrixEngine.ConnMode.UTC_CANDIDATE, w, col, s8);
            ROWS.add(r);
            assertEquals("OK", r.status(), "DATETIME 可存超 TIMESTAMP 上界值: " + r.toJsonLine());
            assertEquals(shiftByColumn(s8.instant(), col), Instant.parse(r.readInstant()),
                    "DATETIME 按声明精度读回: " + r.toJsonLine());
        }
    }

    /** (0) 位列按 MySQL 小数秒 HALF_UP 舍入到秒；(6) 位列保留原值（含微秒） */
    private static Instant shiftByColumn(Instant i, TimeMatrixEngine.Col col) {
        if (col.digits >= 6) {
            return i;
        }
        Instant truncated = i.truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        if (i.getNano() >= 500_000_000) {
            return truncated.plusSeconds(1);
        }
        return truncated;
    }
}
