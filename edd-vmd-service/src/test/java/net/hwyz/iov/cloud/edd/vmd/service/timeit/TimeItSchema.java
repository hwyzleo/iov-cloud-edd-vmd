package net.hwyz.iov.cloud.edd.vmd.service.timeit;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * VMD-DSN-CR-073 G1 隔离库与连接管理（仅测试/脚本使用）。
 * <p>
 * 隔离硬约束（CR-073 §8 与用户确认的第 5 点）：
 * <ul>
 *   <li>schema 必须以 {@link #SCHEMA_PREFIX} 开头，且不得是任何业务库名；create/drop 前强校验；</li>
 *   <li>连接参数从系统属性/环境变量读取，仓库内不含任何口令；</li>
 *   <li>候选 UTC 连接参数 = connectionTimeZone=UTC + forceConnectionTimeZoneToSession=true + preserveInstants=true（RD-TIME-004）；</li>
 *   <li>本类不加载 Nacos、不触碰业务数据源（测试为纯 JDBC，无 Spring 上下文）。</li>
 * </ul>
 */
public final class TimeItSchema {

    /** 隔离 schema 前缀（允许名单），与 CR-073 G1 一致 */
    public static final String SCHEMA_PREFIX = "tmp_time_it_073_";

    /** 业务/系统库拒绝名单（双保险，即使前缀校验通过也不得建/删） */
    private static final String[] FORBIDDEN = {
            "db_vmd", "db_mdm", "db_mpt", "db_ota", "db_tsp", "db_vagw", "db_idk",
            "mysql", "information_schema", "performance_schema", "sys"
    };

    private TimeItSchema() {
    }

    /** 生成唯一隔离库名 */
    public static String newSchemaName() {
        return SCHEMA_PREFIX + System.currentTimeMillis() + "_" + Integer.toHexString(
                (int) (Math.random() * 0xffff));
    }

    /** 强校验：仅允许以隔离前缀开头且非业务库的库名 */
    public static void assertIsolated(String schema) {
        if (schema == null || !schema.matches("^" + SCHEMA_PREFIX + "[0-9a-z_]+$")) {
            throw new IllegalArgumentException("拒绝非隔离库名（CR-073 G1 硬约束）: " + schema);
        }
        for (String f : FORBIDDEN) {
            if (schema.equalsIgnoreCase(f)) {
                throw new IllegalArgumentException("拒绝业务/系统库（CR-073 G1 硬约束）: " + schema);
            }
        }
    }

    public static void createSchema(Connection conn, String schema) throws SQLException {
        assertIsolated(schema);
        try (Statement s = conn.createStatement()) {
            // 幂等（双 JVM 脚本多阶段 write 复用同一隔离库）；仅允许隔离前缀库
            s.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + schema + "` DEFAULT CHARSET=utf8mb4");
        }
    }

    public static void dropSchema(Connection conn, String schema) throws SQLException {
        assertIsolated(schema);
        try (Statement s = conn.createStatement()) {
            s.executeUpdate("DROP DATABASE IF EXISTS `" + schema + "`");
        }
    }

    /** 矩阵表：TIMESTAMP / TIMESTAMP(6) / DATETIME / DATETIME(3) / DATETIME(6) */
    public static void createMatrixTable(Connection conn, String schema) throws SQLException {
        assertIsolated(schema);
        try (Statement s = conn.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS `" + schema + "`.t_time_it ("
                    + "id INT PRIMARY KEY AUTO_INCREMENT, "
                    + "row_key VARCHAR(120) NOT NULL, "
                    + "c_ts TIMESTAMP NULL, c_ts6 TIMESTAMP(6) NULL, "
                    + "c_dt DATETIME NULL, c_dt3 DATETIME(3) NULL, c_dt6 DATETIME(6) NULL) "
                    + "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        }
    }

    public static String jdbcBase(String schema) {
        assertIsolated(schema);
        String host = System.getProperty("time.it.mysql.host", "127.0.0.1");
        String port = System.getProperty("time.it.mysql.port", "3306");
        return "jdbc:mysql://" + host + ":" + port + "/" + schema
                + "?useUnicode=true&characterEncoding=utf8&allowMultiQueries=true"
                + "&allowPublicKeyRetrieval=true&useSSL=false";
    }

    /** 管理连接（无 schema）：仅用于建/删隔离库，不指向任何业务库 */
    public static String jdbcServer() {
        String host = System.getProperty("time.it.mysql.host", "127.0.0.1");
        String port = System.getProperty("time.it.mysql.port", "3306");
        return "jdbc:mysql://" + host + ":" + port + "/?useUnicode=true&characterEncoding=utf8"
                + "&allowPublicKeyRetrieval=true&useSSL=false";
    }

    /** 默认连接：与生产 Nacos mysql.yaml 相同的参数形态（无时区参数）——风险复现组 */
    public static String defaultUrl(String schema) {
        return jdbcBase(schema);
    }

    /** 候选 UTC 连接（RD-TIME-004）：connectionTimeZone=UTC + forceConnectionTimeZoneToSession + preserveInstants */
    public static String utcCandidateUrl(String schema) {
        return jdbcBase(schema)
                + "&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&preserveInstants=true";
    }

    public static String user() {
        return System.getProperty("time.it.mysql.user", "root");
    }

    public static String password() {
        String p = System.getProperty("time.it.mysql.password");
        if (p == null || p.isEmpty()) {
            p = System.getenv().getOrDefault("VMD_IT_MYSQL_PASSWORD", "");
        }
        return p;
    }

    public static boolean mysqlReachable() {
        String host = System.getProperty("time.it.mysql.host", "127.0.0.1");
        String port = System.getProperty("time.it.mysql.port", "3306");
        try (Connection c = DriverManager.getConnection(
                "jdbc:mysql://" + host + ":" + port + "/mysql"
                        + "?useUnicode=true&characterEncoding=utf8&allowPublicKeyRetrieval=true&useSSL=false",
                user(), password())) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public static Connection open(String url) throws SQLException {
        return DriverManager.getConnection(url, user(), password());
    }

    /** 服务器会话/全局/系统时区（三值） */
    public static String[] serverTz(Connection conn) throws SQLException {
        try (Statement s = conn.createStatement();
             ResultSet r = s.executeQuery("SELECT @@session.time_zone, @@global.time_zone, @@system_time_zone")) {
            r.next();
            return new String[]{r.getString(1), r.getString(2), r.getString(3)};
        }
    }

    public static String jvmTz() {
        return java.util.TimeZone.getDefault().getID();
    }
}
