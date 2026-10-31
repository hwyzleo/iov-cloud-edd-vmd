package net.hwyz.iov.cloud.edd.vmd.service.timeit;

import net.hwyz.iov.cloud.framework.mysql.typehandler.UtcInstantTypeHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * CR-073 用户确认第 4 点：先核验框架 {@code UtcInstantTypeHandler} 的精度能力。
 * <p>
 * 若现有框架 handler 固定截到毫秒，本测试将失败——如实记录其不适用于 (6)，
 * 不得让微秒测试降低预期，也不得在 VMD 复制一套长期 handler（框架扩展另走其变更）。
 * 本测试只核验与报告缺口，不改框架代码。
 */
@DisplayName("CR-073 G1 框架 UtcInstantTypeHandler 精度核验（隔离库）")
@EnabledIfSystemProperty(named = "time.it.mysql.host", matches = ".*", disabledReason = "仅显式指定 MySQL 时执行")
class UtcInstantTypeHandlerPrecisionIT {

    private static String schema;

    @BeforeAll
    static void init() throws Exception {
        assumeTrue(TimeItSchema.mysqlReachable(), "MySQL 不可达：本 IT 未执行（不得验收通过）");
        schema = TimeItSchema.newSchemaName();
        try (Connection admin = TimeItSchema.open(TimeItSchema.jdbcServer())) {
            TimeItSchema.createSchema(admin, schema);
        }
        try (Connection c = TimeItSchema.open(TimeItSchema.utcCandidateUrl(schema))) {
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
    }

    @Test
    @DisplayName("框架 handler 在 DATETIME(6) 上必须保留微秒（不得固定毫秒）")
    void frameworkHandler_microsOnDatetime6() throws SQLException {
        Instant micro = Instant.parse("2026-10-30T00:00:00.123456Z");
        UtcInstantTypeHandler handler = new UtcInstantTypeHandler();
        try (Connection c = TimeItSchema.open(TimeItSchema.utcCandidateUrl(schema));
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO `" + schema + "`.t_time_it (row_key, c_dt6) VALUES ('h1', ?)")) {
            // setNonNullParameter 语义：LocalDateTime.ofInstant(instant, UTC) → setObject(TIMESTAMP)
            handler.setNonNullParameter(ps, 1, micro, null);
            ps.executeUpdate();
        }
        try (Connection c = TimeItSchema.open(TimeItSchema.utcCandidateUrl(schema));
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(
                     "SELECT c_dt6 FROM `" + schema + "`.t_time_it WHERE row_key='h1'")) {
            rs.next();
            // 原始展示（UTC 核验连接）：必须保留微秒
            String wall = rs.getString(1);
            assertEquals("2026-10-30 00:00:00.123456", wall,
                    "框架 handler 不得把 (6) 截到毫秒；若失败=框架能力缺口证据（CR-073 用户确认第4点）");
            Instant back = handler.getNullableResult(rs, 1);
            assertEquals(micro, back, "框架 handler 读取应逐位还原微秒");
        }
    }

    @Test
    @DisplayName("框架 handler 在 DATETIME(3) 上按毫秒声明精度截断（预期）")
    void frameworkHandler_millisOnDatetime3() throws SQLException {
        Instant micro = Instant.parse("2026-10-30T00:00:00.123456Z");
        UtcInstantTypeHandler handler = new UtcInstantTypeHandler();
        try (Connection c = TimeItSchema.open(TimeItSchema.utcCandidateUrl(schema));
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO `" + schema + "`.t_time_it (row_key, c_dt3) VALUES ('h2', ?)")) {
            handler.setNonNullParameter(ps, 1, micro, null);
            ps.executeUpdate();
        }
        try (Connection c = TimeItSchema.open(TimeItSchema.utcCandidateUrl(schema));
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(
                     "SELECT c_dt3 FROM `" + schema + "`.t_time_it WHERE row_key='h2'")) {
            rs.next();
            assertEquals("2026-10-30 00:00:00.123", rs.getString(1),
                    "(3) 按毫秒声明精度验收");
            assertEquals(micro.truncatedTo(java.time.temporal.ChronoUnit.MILLIS), handler.getNullableResult(rs, 1));
        }
    }

    @Test
    @DisplayName("框架 handler 读取空值为 null（不转 epoch 0）")
    void frameworkHandler_null() throws SQLException {
        UtcInstantTypeHandler handler = new UtcInstantTypeHandler();
        try (Connection c = TimeItSchema.open(TimeItSchema.utcCandidateUrl(schema));
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(
                     "SELECT NULL AS c_dt6")) {
            rs.next();
            assertEquals(null, handler.getNullableResult(rs, 1), "null 保持可空语义");
        }
    }

    @Test
    @DisplayName("框架 handler 写读一致性：LocalDateTime 读路径（getObject 还原）")
    void frameworkHandler_readPathLdt() throws SQLException {
        Instant micro = Instant.parse("2026-10-30T00:00:00.123456Z");
        try (Connection c = TimeItSchema.open(TimeItSchema.utcCandidateUrl(schema));
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO `" + schema + "`.t_time_it (row_key, c_dt6) VALUES ('h3', ?)")) {
            ps.setObject(1, LocalDateTime.ofInstant(micro, java.time.ZoneOffset.UTC), java.sql.Types.TIMESTAMP);
            ps.executeUpdate();
        }
        try (Connection c = TimeItSchema.open(TimeItSchema.utcCandidateUrl(schema));
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(
                     "SELECT c_dt6 FROM `" + schema + "`.t_time_it WHERE row_key='h3'")) {
            rs.next();
            LocalDateTime ldt = rs.getObject(1, LocalDateTime.class);
            assertEquals(LocalDateTime.of(2026, 10, 30, 0, 0, 0, 123_456_000), ldt,
                    "UTC 墙钟读取（微秒保留）");
            assertEquals(micro, ldt.toInstant(java.time.ZoneOffset.UTC), "按 UTC 解释还原绝对时刻");
        }
    }
}
