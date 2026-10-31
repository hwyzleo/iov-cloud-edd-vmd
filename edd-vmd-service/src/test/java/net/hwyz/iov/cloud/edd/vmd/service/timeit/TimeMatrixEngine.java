package net.hwyz.iov.cloud.edd.vmd.service.timeit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * CR-073 TC-TIME-02/03 矩阵引擎：写路径 × 连接模式 × 列类型 × 样本。
 * <p>
 * 用户确认要点：
 * <ul>
 *   <li>默认 URL/旧 handler = 风险复现组，候选 UTC 路径 = 目标验收组，分别断言预期偏移/精度损失或正确往返；</li>
 *   <li>证据须同时记录：写入/读取 JVM TZ、两端 @@session.time_zone、列类型/精度、JDBC setter/getter、
 *       固定 UTC 核验连接下的展示值及还原的 Instant（getString() 不等于原始存储值）；</li>
 *   <li>负 epoch / 超 TIMESTAMP 范围样本验证拒绝/范围限制，而非要求成功存储。</li>
 * </ul>
 */
public final class TimeMatrixEngine {

    public enum Col {
        TS("c_ts", "TIMESTAMP", 0),
        TS6("c_ts6", "TIMESTAMP(6)", 6),
        DT("c_dt", "DATETIME", 0),
        DT3("c_dt3", "DATETIME(3)", 3),
        DT6("c_dt6", "DATETIME(6)", 6);

        final String column;
        final String type;
        final int digits;

        Col(String column, String type, int digits) {
            this.column = column;
            this.type = type;
            this.digits = digits;
        }
    }

    /** JDBC 写路径：分别对应 MyBatis InstantTypeHandler / LocalDateTimeTypeHandler / FW UtcInstantTypeHandler / DateTypeHandler */
    public enum Writer {
        INSTANT_SET_TIMESTAMP("MyBatis-InstantTypeHandler(setTimestamp)"),
        LDT_SET_OBJECT("MyBatis-LocalDateTimeTypeHandler(setObject)"),
        UTC_WALL_SET_OBJECT("FW-UtcInstantTypeHandler(setObject(UTC墙, TIMESTAMP))"),
        DATE_SET_TIMESTAMP("MyBatis-DateTypeHandler(setTimestamp)");

        final String desc;

        Writer(String desc) {
            this.desc = desc;
        }
    }

    public enum ConnMode {
        DEFAULT("默认URL(无时区参数,风险复现组)", "default"),
        UTC_CANDIDATE("候选UTC(connectionTimeZone=UTC&forceSession&preserveInstants,验收组)", "utc");

        final String desc;
        final String tag;

        ConnMode(String desc, String tag) {
            this.desc = desc;
            this.tag = tag;
        }

        public String url(String schema) {
            return this == DEFAULT ? TimeItSchema.defaultUrl(schema) : TimeItSchema.utcCandidateUrl(schema);
        }
    }

    /** 单行写入+核验结果（结构化证据，序列化为 JSON 行供脚本/文档使用） */
    public record Row(
            String rowKey, String jvmTz, String connMode, String writer, String col, String colType, int colDigits,
            String sample, String sampleInstant, String writeSessionTz, String verifySessionTz,
            String storedWallUtcCheck, String readInstant, String readLdt, String expectedInstant,
            String status, String detail) {

        public String toJsonLine() {
            return "{\"rowKey\":\"" + rowKey + "\",\"jvmTz\":\"" + jvmTz + "\",\"connMode\":\"" + connMode
                    + "\",\"writer\":\"" + writer + "\",\"col\":\"" + col + "\",\"colType\":\"" + colType
                    + "\",\"colDigits\":" + colDigits + ",\"sample\":\"" + sample + "\",\"sampleInstant\":\""
                    + sampleInstant + "\",\"writeSessionTz\":\"" + writeSessionTz + "\",\"verifySessionTz\":\""
                    + verifySessionTz + "\",\"storedWallUtcCheck\":\"" + storedWallUtcCheck
                    + "\",\"readInstant\":\"" + readInstant + "\",\"readLdt\":\"" + readLdt
                    + "\",\"expectedInstant\":\"" + expectedInstant + "\",\"status\":\"" + status
                    + "\",\"detail\":\"" + detail + "\"}";
        }
    }

    private TimeMatrixEngine() {
    }

    /** 写入一个样本到指定列并立即用 UTC 核验连接读取（写路径 + 核验证据），rowKey 由调用方指定（可含 JVM 标签） */
    public static Row writeAndVerify(String schema, ConnMode mode, Writer writer, Col col, TimeSample sample,
                                     String rowKey) {
        String jvmTz = TimeItSchema.jvmTz();
        String writeSessionTz;
        String verifySessionTz;
        String storedWall;
        String readInstant;
        String readLdt;
        String status = "OK";
        String detail = "";
        String url = mode.url(schema);
        try (Connection wc = TimeItSchema.open(url)) {
            writeSessionTz = String.join("/", TimeItSchema.serverTz(wc));
            insert(wc, schema, rowKey, col, writer, sample);
        } catch (SQLException e) {
            writeSessionTz = "N/A";
            storedWall = null;
            readInstant = null;
            readLdt = null;
            status = "REJECTED";
            detail = safeDetail(e);
            return new Row(rowKey, jvmTz, mode.name(), writer.name(), col.column, col.type, col.digits,
                    sample.name(), str(sample.instant()), writeSessionTz, "", "", "", "", str(sample.instant()),
                    status, detail);
        }
        try (Connection vc = TimeItSchema.open(TimeItSchema.utcCandidateUrl(schema))) {
            verifySessionTz = String.join("/", TimeItSchema.serverTz(vc));
            try (Statement s = vc.createStatement();
                 ResultSet r = s.executeQuery(
                         "SELECT " + col.column + " FROM `" + schema + "`.t_time_it WHERE row_key = '" + rowKey + "'")) {
                if (r.next()) {
                    storedWall = r.getString(1);
                    Timestamp ts = r.getTimestamp(1);
                    readInstant = ts == null ? "null" : ts.toInstant().toString();
                    LocalDateTime ldt = r.getObject(1, LocalDateTime.class);
                    readLdt = String.valueOf(ldt);
                } else {
                    storedWall = null;
                    readInstant = null;
                    readLdt = null;
                    status = "NOT_FOUND";
                    detail = "row_key 未找到";
                }
            }
        } catch (SQLException e) {
            verifySessionTz = "N/A";
            storedWall = null;
            readInstant = null;
            readLdt = null;
            status = "VERIFY_ERR";
            detail = safeDetail(e);
        }
        return new Row(rowKey, jvmTz, mode.name(), writer.name(), col.column, col.type, col.digits,
                sample.name(), str(sample.instant()), writeSessionTz, verifySessionTz,
                storedWall, readInstant, readLdt, str(sample.instant()), status, detail);
    }

    /** 单 JVM 默认 rowKey（IT 使用，无 JVM 标签无冲突） */
    public static Row writeAndVerify(String schema, ConnMode mode, Writer writer, Col col, TimeSample sample) {
        return writeAndVerify(schema, mode, writer, col, sample,
                mode.tag + "|" + writer + "|" + col.column + "|" + sample.name());
    }

    /** 纯读取（供双 JVM 交叉读：JVM B 读 JVM A 写入的 fixture） */
    public static Row readOnly(String schema, String rowKey, Col col, TimeSample sample) {
        String jvmTz = TimeItSchema.jvmTz();
        try (Connection vc = TimeItSchema.open(TimeItSchema.utcCandidateUrl(schema))) {
            String verifySessionTz = String.join("/", TimeItSchema.serverTz(vc));
            try (Statement s = vc.createStatement();
                 ResultSet r = s.executeQuery(
                         "SELECT " + col.column + " FROM `" + schema + "`.t_time_it WHERE row_key = '" + rowKey + "'")) {
                if (r.next()) {
                    String storedWall = r.getString(1);
                    Timestamp ts = r.getTimestamp(1);
                    String readInstant = ts == null ? "null" : ts.toInstant().toString();
                    LocalDateTime ldt = r.getObject(1, LocalDateTime.class);
                    return new Row(rowKey, jvmTz, "READ_ONLY", "READ_ONLY", col.column, col.type, col.digits,
                            sample.name(), str(sample.instant()), "", verifySessionTz,
                            storedWall, readInstant, String.valueOf(ldt), str(sample.instant()), "OK", "");
                }
            }
        } catch (SQLException e) {
            return new Row(rowKey, jvmTz, "READ_ONLY", "READ_ONLY", col.column, col.type, col.digits,
                    sample.name(), str(sample.instant()), "", "", "", "", "", str(sample.instant()),
                    "READ_ERR", safeDetail(e));
        }
        return new Row(rowKey, jvmTz, "READ_ONLY", "READ_ONLY", col.column, col.type, col.digits,
                sample.name(), str(sample.instant()), "", "", "", "", "", str(sample.instant()),
                "NOT_FOUND", "row_key 未找到");
    }

    private static void insert(Connection wc, String schema, String rowKey, Col col, Writer writer, TimeSample sample)
            throws SQLException {
        String sql = "INSERT INTO `" + schema + "`.t_time_it (row_key, " + col.column + ") VALUES (?, ?)";
        try (PreparedStatement ps = wc.prepareStatement(sql)) {
            ps.setString(1, rowKey);
            switch (writer) {
                case INSTANT_SET_TIMESTAMP -> ps.setTimestamp(2, Timestamp.from(sample.instant()));
                case LDT_SET_OBJECT -> ps.setObject(2, sample.ldtWall());
                case UTC_WALL_SET_OBJECT ->
                        ps.setObject(2, LocalDateTime.ofInstant(sample.instant(), ZoneOffset.UTC), Types.TIMESTAMP);
                case DATE_SET_TIMESTAMP -> ps.setTimestamp(2, new Timestamp(java.util.Date.from(sample.instant()).getTime()));
                default -> throw new IllegalStateException("未知 writer: " + writer);
            }
            ps.executeUpdate();
        }
    }

    private static String str(Instant i) {
        return i == null ? "null" : i.toString();
    }

    private static String safeDetail(SQLException e) {
        return e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage()).replace("\"", "'").substring(
                0, Math.min(160, String.valueOf(e.getMessage()).length()));
    }
}
