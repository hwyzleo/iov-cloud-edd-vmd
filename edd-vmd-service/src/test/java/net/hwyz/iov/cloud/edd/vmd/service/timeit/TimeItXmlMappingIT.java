package net.hwyz.iov.cloud.edd.vmd.service.timeit;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * CR-073 G1 候选 PO/XML 映射实测（TC-TIME-05 子集，隔离库）。
 * <p>
 * 用原生 MyBatis 装配真实 XML（resultMap + 参数 typeHandler），验证：
 * <ul>
 *   <li>显式 typeHandler（FW UtcInstantTypeHandler）在 (6) 上微秒往返；</li>
 *   <li>MyBatis 默认 InstantTypeHandler（setTimestamp）在候选 UTC 连接下的行为（对照/记录）；</li>
 *   <li>LDT 墙钟原样；纯墙钟不注册 UTC 转换。</li>
 * </ul>
 * 不启动 Spring、不加载 Nacos；MP 注解（@TableField typeHandler）运行时装配验证留待服务级装配。
 */
@DisplayName("CR-073 G1 候选 PO/XML 映射（隔离库）")
@EnabledIfSystemProperty(named = "time.it.mysql.host", matches = ".*", disabledReason = "仅显式指定 MySQL 时执行")
class TimeItXmlMappingIT {

    private static String schema;
    private static SqlSessionFactory factory;

    @BeforeAll
    static void init() throws Exception {
        assumeTrue(TimeItSchema.mysqlReachable(), "MySQL 不可达：本 IT 未执行（不得验收通过）");
        schema = TimeItSchema.newSchemaName();
        try (var admin = TimeItSchema.open(TimeItSchema.jdbcServer())) {
            TimeItSchema.createSchema(admin, schema);
        }
        try (var c = TimeItSchema.open(TimeItSchema.utcCandidateUrl(schema))) {
            TimeItSchema.createMatrixTable(c, schema);
        }
        UnpooledDataSource ds = new UnpooledDataSource(
                "com.mysql.cj.jdbc.Driver",
                TimeItSchema.utcCandidateUrl(schema), TimeItSchema.user(), TimeItSchema.password());
        Configuration cfg = new Configuration();
        cfg.setMapUnderscoreToCamelCase(true);
        cfg.setEnvironment(new Environment("timeit", new JdbcTransactionFactory(), ds));
        cfg.addMapper(TimeItMapper.class);
        try (InputStream in = TimeItXmlMappingIT.class.getClassLoader()
                .getResourceAsStream("mappers-timeit/TimeItMapper.xml")) {
            XMLMapperBuilder builder = new XMLMapperBuilder(in, cfg, "mappers-timeit/TimeItMapper.xml", cfg.getSqlFragments());
            builder.parse();
        }
        factory = new SqlSessionFactoryBuilder().build(cfg);
    }

    @AfterAll
    static void cleanup() throws Exception {
        if (schema != null) {
            try (var admin = TimeItSchema.open(TimeItSchema.jdbcServer())) {
                TimeItSchema.dropSchema(admin, schema);
            }
        }
    }

    @Test
    @DisplayName("XML 显式 typeHandler：Instant→(6) 微秒往返，resultMap 读取生效")
    void xml_utcHandler_microRoundTrip() {
        Instant micro = Instant.parse("2026-10-30T00:00:00.123456Z");
        try (SqlSession s = factory.openSession()) {
            TimeItMapper m = s.getMapper(TimeItMapper.class);
            Map<String, Object> p = new HashMap<>();
            p.put("rowKey", "xml-utc-h");
            p.put("dt6", micro);
            m.insertWithUtcHandler(p);
            TimeItPo po = m.selectByRowKey("xml-utc-h");
            assertEquals(micro, po.getDt6(), "XML resultMap typeHandler 必须逐位还原微秒（候选验收组）");
        }
    }

    @Test
    @DisplayName("XML 默认 InstantTypeHandler 对照：候选 UTC 连接下 setTimestamp 行为（记录）")
    void xml_defaultInstantHandler_underUtcConnection() {
        Instant micro = Instant.parse("2026-10-30T00:00:00.123456Z");
        try (SqlSession s = factory.openSession()) {
            TimeItMapper m = s.getMapper(TimeItMapper.class);
            Map<String, Object> p = new HashMap<>();
            p.put("rowKey", "xml-default-h");
            p.put("dt6", micro);
            m.insertWithDefaultInstant(p);
            TimeItPo po = m.selectByRowKey("xml-default-h");
            // 候选连接强制 UTC 会话 + 驱动 connectionTimeZone=UTC：默认 InstantTypeHandler(setTimestamp) 也应绝对正确
            assertEquals(micro, po.getDt6(),
                    "候选 UTC 连接下 MyBatis 默认 InstantTypeHandler 行为（对照证据）");
            System.out.println("[CR073-G1][记录] XML 默认 handler + 候选 UTC 连接 = 绝对正确（连接层修复证据）");
        }
    }

    @Test
    @DisplayName("XML LDT 墙钟原样（(6) 微秒 / (3) 毫秒 / (0) 秒，按声明精度）")
    void xml_ldtWall_andMilli() throws Exception {
        LocalDateTime wall = LocalDateTime.of(2026, 10, 30, 8, 0, 0, 123_456_000);
        try (var c = TimeItSchema.open(TimeItSchema.utcCandidateUrl(schema));
             var ps = c.prepareStatement(
                     "INSERT INTO `" + schema + "`.t_time_it (row_key, c_dt6, c_dt3, c_dt) VALUES ('xml-ldt', ?, ?, ?)")) {
            ps.setObject(1, wall);
            ps.setObject(2, wall);
            ps.setObject(3, wall);
            ps.executeUpdate();
        }
        try (SqlSession s = factory.openSession()) {
            TimeItPo po = s.getMapper(TimeItMapper.class).selectByRowKey("xml-ldt");
            assertEquals(wall.withNano(wall.getNano() / 1_000_000 * 1_000_000), po.getDt3(),
                    "(3) 按毫秒声明精度");
            assertEquals(wall.truncatedTo(java.time.temporal.ChronoUnit.SECONDS), po.getDt(),
                    "(0) 按秒声明精度");
            // 候选映射：c_dt6 经 UtcInstantTypeHandler 读取 → 墙钟按 UTC 解释还原为 Instant
            assertEquals(wall.toInstant(java.time.ZoneOffset.UTC), po.getDt6(),
                    "UtcInstantTypeHandler 按 UTC 解释 (6) 墙钟（候选映射语义）");
        }
    }
}
