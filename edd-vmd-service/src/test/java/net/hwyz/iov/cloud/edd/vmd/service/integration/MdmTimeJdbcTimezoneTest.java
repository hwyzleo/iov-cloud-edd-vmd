package net.hwyz.iov.cloud.edd.vmd.service.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VMD-DSN-CR-075 T06 双 JVM 时区：纯 JDBC 时间往返（不经 Spring 探针）
 *
 * <p>以 UTC 与 Asia/Shanghai 两种 JVM 时区各运行一次（mvn -DargLine="-Duser.timezone=..."），
 * 验证 JDBC 层（connectionTimeZone=UTC + forceConnectionTimeZoneToSession=true +
 * preserveInstants=true，与 VMD 生产 JDBC URL 一致）的时间映射与时区无关：DB now() 生成的
 * DATETIME 读回 Date.getTime()（epoch 毫秒）与当前 UTC epoch 对齐、与 SQL 层 UNIX_TIMESTAMP 一致，
 * 在任意 JVM 时区下成立。</p>
 *
 * <p>Spring 探针强制生产 JVM UTC（iov.mysql.time.enabled=true，FW-MYSQL-DSN-CR-001），
 * 第二时区集成测试由本纯 JDBC 测试承载，不绕开探针。</p>
 *
 * @author hwyz_leo
 */
class MdmTimeJdbcTimezoneTest {

    private static final String URL = System.getProperty("vmd.test.jdbc.url",
            "jdbc:mysql://127.0.0.1:3306/db_vmd?useUnicode=true&characterEncoding=utf8&allowMultiQueries=true"
                    + "&allowPublicKeyRetrieval=true&useSSL=false&connectionTimeZone=UTC"
                    + "&forceConnectionTimeZoneToSession=true&preserveInstants=true");
    private static final String USER = System.getProperty("vmd.test.jdbc.user", "root");
    private static final String PASS = System.getProperty("vmd.test.jdbc.pass", "9jpHx:G9NBM@Wst");

    @Test
    @DisplayName("DB now() 生成时间在任意 JVM 时区下读回一致（epoch 对齐 + SQL 层对齐）")
    void dbNowTimeRoundTrip_isTimezoneIndependent() throws Exception {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             Statement st = conn.createStatement()) {
            st.execute("DELETE FROM tb_mdm_brand WHERE code = 'TEST_TIME_TZ'");
            // DB now() 生成（会话 UTC +00:00，连接初始化 SQL 由框架统一设置）
            st.executeUpdate("INSERT INTO tb_mdm_brand "
                    + "(code, name, enable, sort, source, create_time, create_by, modify_time, modify_by, row_version, row_valid) "
                    + "VALUES ('TEST_TIME_TZ', '时区测试', 1, 1, 'MANUAL', now(), NULL, now(), NULL, 1, 1)");
            long insertedAt = System.currentTimeMillis();

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, create_time, modify_time, row_version, row_valid FROM tb_mdm_brand WHERE code = 'TEST_TIME_TZ'")) {
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    Date createTime = rs.getTimestamp("create_time");
                    Date modifyTime = rs.getTimestamp("modify_time");
                    assertNotNull(createTime);
                    assertNotNull(modifyTime);

                    // 断言 1：DB now() 来源（epoch 对齐，±10 秒）——与时区无关
                    assertTrue(Math.abs(createTime.getTime() - insertedAt) < 10_000L,
                            "create_time 应为 DB now()（UTC 会话），偏差: " + (createTime.getTime() - insertedAt));
                    assertTrue(Math.abs(modifyTime.getTime() - insertedAt) < 10_000L);

                    // 断言 2：SQL 层 UNIX_TIMESTAMP（DB 解释）与 JDBC 读值一致（±2 秒）——时区无关往返
                    try (PreparedStatement ps2 = conn.prepareStatement(
                            "SELECT UNIX_TIMESTAMP(create_time) * 1000 FROM tb_mdm_brand WHERE id = ?")) {
                        ps2.setLong(1, rs.getLong("id"));
                        try (ResultSet rs2 = ps2.executeQuery()) {
                            assertTrue(rs2.next());
                            long sqlEpoch = rs2.getLong(1);
                            assertTrue(Math.abs(sqlEpoch - createTime.getTime()) < 2000L,
                                    "SQL 层与 JDBC 读值应一致: " + (sqlEpoch - createTime.getTime()));
                        }
                    }
                    assertEquals(1, rs.getInt("row_version"));
                    assertTrue(rs.getBoolean("row_valid"));
                }
            }
            st.execute("DELETE FROM tb_mdm_brand WHERE code = 'TEST_TIME_TZ'");
        }
    }
}
