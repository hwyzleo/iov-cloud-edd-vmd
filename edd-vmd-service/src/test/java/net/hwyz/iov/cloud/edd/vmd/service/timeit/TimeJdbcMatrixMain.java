package net.hwyz.iov.cloud.edd.vmd.service.timeit;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * CR-073 TC-TIME-02 双 JVM 交叉验证入口（由 run_time_matrix.sh 调用，UTC/Asia/Shanghai 两个 JVM 各写各读）。
 * <p>
 * 用法：TimeJdbcMatrixMain write|read|drop &lt;schema&gt;
 * <ul>
 *   <li>write：建隔离库与矩阵表，写候选验收组（UTC_CANDIDATE × 3 writer × (6) 列 × 关键样本）与
 *       默认风险复现组（DEFAULT × Instant/Date writer），输出 Row JSON 行（stdout）；</li>
 *   <li>read：按同一 row_key 集合用 UTC 核验连接只读，输出 Row JSON 行；</li>
 *   <li>drop：按 CR-073 硬约束删除隔离库（校验前缀）。</li>
 * </ul>
 * 交叉读：JVM A write → JVM B read 同一 schema；脚本比对候选组 readInstant==expected（验收）
 * 与默认组差异清单（记录，不因旧路径失败）。
 */
public final class TimeJdbcMatrixMain {

    public static void main(String[] args) throws Exception {
        String phase = args[0];
        String schema = args[1];
        TimeItSchema.assertIsolated(schema);
        switch (phase) {
            case "write" -> {
                try (Connection admin = TimeItSchema.open(TimeItSchema.jdbcServer())) {
                    TimeItSchema.createSchema(admin, schema);
                }
                try (Connection c = TimeItSchema.open(TimeItSchema.defaultUrl(schema))) {
                    TimeItSchema.createMatrixTable(c, schema);
                }
                for (TimeMatrixEngine.Row r : writeRows(schema)) {
                    System.out.println(r.toJsonLine());
                }
            }
            case "read" -> {
                for (TimeMatrixEngine.Row r : readRows(schema)) {
                    System.out.println(r.toJsonLine());
                }
            }
            case "drop" -> {
                try (Connection admin = TimeItSchema.open(TimeItSchema.jdbcServer())) {
                    TimeItSchema.dropSchema(admin, schema);
                }
                System.out.println("{\"phase\":\"drop\",\"schema\":\"" + schema + "\"}");
            }
            default -> throw new IllegalArgumentException("phase 必须为 write|read|drop");
        }
    }

    /** 写阶段固定行集合（脚本据此重建 read 集合；row_key 含写入 JVM 标签避免双 JVM 冲突） */
    private static List<TimeMatrixEngine.Row> writeRows(String schema) {
        List<TimeMatrixEngine.Row> rows = new ArrayList<>();
        TimeSample[] samples = TimeSample.all();
        String jvm = jvmKey();
        // 候选验收组：UTC_CANDIDATE × {Instant/LDT-UTC墙/Date} × {(6)} × 关键样本
        for (TimeMatrixEngine.Writer w : new TimeMatrixEngine.Writer[]{
                TimeMatrixEngine.Writer.INSTANT_SET_TIMESTAMP,
                TimeMatrixEngine.Writer.UTC_WALL_SET_OBJECT,
                TimeMatrixEngine.Writer.DATE_SET_TIMESTAMP}) {
            for (TimeMatrixEngine.Col col : new TimeMatrixEngine.Col[]{
                    TimeMatrixEngine.Col.DT6, TimeMatrixEngine.Col.TS6}) {
                for (TimeSample s : samples) {
                    rows.add(TimeMatrixEngine.writeAndVerify(schema, TimeMatrixEngine.ConnMode.UTC_CANDIDATE, w, col, s,
                            rowKey(TimeMatrixEngine.ConnMode.UTC_CANDIDATE, w, col, s, jvm)));
                }
            }
        }
        // 默认风险复现组（记录/条件断言）
        for (TimeMatrixEngine.Writer w : new TimeMatrixEngine.Writer[]{
                TimeMatrixEngine.Writer.INSTANT_SET_TIMESTAMP, TimeMatrixEngine.Writer.DATE_SET_TIMESTAMP}) {
            for (TimeMatrixEngine.Col col : new TimeMatrixEngine.Col[]{TimeMatrixEngine.Col.DT6, TimeMatrixEngine.Col.TS6}) {
                rows.add(TimeMatrixEngine.writeAndVerify(schema, TimeMatrixEngine.ConnMode.DEFAULT, w, col,
                        samples[2] /* S3_MICRO */,
                        rowKey(TimeMatrixEngine.ConnMode.DEFAULT, w, col, samples[2], jvm)));
            }
        }
        return rows;
    }

    /** 读阶段：重建与 write 相同的 row_key 集合（候选组全样本 + 默认组 S3，覆盖两个写入 JVM），只读核验 */
    private static List<TimeMatrixEngine.Row> readRows(String schema) {
        List<TimeMatrixEngine.Row> rows = new ArrayList<>();
        TimeSample[] samples = TimeSample.all();
        for (String jvm : new String[]{"UTC", "Asia_Shanghai"}) {
            for (TimeMatrixEngine.Writer w : new TimeMatrixEngine.Writer[]{
                    TimeMatrixEngine.Writer.INSTANT_SET_TIMESTAMP,
                    TimeMatrixEngine.Writer.UTC_WALL_SET_OBJECT,
                    TimeMatrixEngine.Writer.DATE_SET_TIMESTAMP}) {
                for (TimeMatrixEngine.Col col : new TimeMatrixEngine.Col[]{
                        TimeMatrixEngine.Col.DT6, TimeMatrixEngine.Col.TS6}) {
                    for (TimeSample s : samples) {
                        rows.add(TimeMatrixEngine.readOnly(schema,
                                rowKey(TimeMatrixEngine.ConnMode.UTC_CANDIDATE, w, col, s, jvm), col, s));
                    }
                }
            }
            for (TimeMatrixEngine.Writer w : new TimeMatrixEngine.Writer[]{
                    TimeMatrixEngine.Writer.INSTANT_SET_TIMESTAMP, TimeMatrixEngine.Writer.DATE_SET_TIMESTAMP}) {
                for (TimeMatrixEngine.Col col : new TimeMatrixEngine.Col[]{TimeMatrixEngine.Col.DT6, TimeMatrixEngine.Col.TS6}) {
                    TimeSample s = samples[2];
                    rows.add(TimeMatrixEngine.readOnly(schema,
                            rowKey(TimeMatrixEngine.ConnMode.DEFAULT, w, col, s, jvm), col, s));
                }
            }
        }
        return rows;
    }

    static String jvmKey() {
        return TimeItSchema.jvmTz().replace("/", "_");
    }

    static String rowKey(TimeMatrixEngine.ConnMode mode, TimeMatrixEngine.Writer w, TimeMatrixEngine.Col col,
                         TimeSample s, String jvm) {
        return mode.tag + "|" + w + "|" + col.column + "|" + s.name() + "|" + jvm;
    }
}
