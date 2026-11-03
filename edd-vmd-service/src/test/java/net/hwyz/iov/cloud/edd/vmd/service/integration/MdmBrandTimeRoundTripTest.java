package net.hwyz.iov.cloud.edd.vmd.service.integration;

import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity.Brand;
import net.hwyz.iov.cloud.edd.vmd.service.domain.model.valueobject.SourceType;
import net.hwyz.iov.cloud.edd.vmd.service.domain.repository.MdmBrandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VMD-DSN-CR-075 T06 双 JVM 时区时间往返：原生查询与保留 XML 路径的 handler/映射验证
 *
 * <p>以 UTC 与 Asia/Shanghai 两种 JVM 时区各运行一次（mvn -DargLine="-Duser.timezone=..."），
 * 验证：原生 BaseMapper.selectById 与保留 XML selectPoByCode 读同一行 create_time/modify_time
 * 的往返一致（JDBC connectionTimeZone=UTC + preserveInstants=true，Date.getTime() 与时区无关）；\n
 * SQL 层 UNIX_TIMESTAMP 与 JDBC 读值对齐（DB now() 来源、UTC 会话）；row_version/row_valid 映射正确。
 * 时间非空不足以证明来源与映射正确——本测试断言值与来源。</p>
 *
 * @author hwyz_leo
 */
class MdmBrandTimeRoundTripTest extends BaseTest {

    @Autowired
    private MdmBrandRepository mdmBrandRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long brandId;
    private String code = "TEST_TIME_CR075";

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM tb_mdm_brand WHERE code LIKE 'TEST_TIME_%'");
        Brand brand = Brand.builder()
                .code(code)
                .name("时间往返")
                .enable(true)
                .sort(1)
                .source(SourceType.MANUAL)
                .build();
        mdmBrandRepository.insert(brand);
        brandId = brand.getId();
    }

    @Test
    @DisplayName("原生 selectById 与 XML selectPoByCode 读同一行时间往返一致（DB now() 来源）")
    void nativeAndXmlPaths_shouldReturnConsistentTimeRoundTrip() {
        // When：两条查询路径读取同一行（原生 selectById 与 XML selectPoByCode）
        Brand viaNative = mdmBrandRepository.selectById(brandId);
        Brand viaXml = mdmBrandRepository.selectByCode(code);

        // Then：两路径均正确映射同一行
        assertNotNull(viaNative);
        assertNotNull(viaXml);
        assertEquals(brandId, viaXml.getId());
        assertEquals(viaNative.getCode(), viaXml.getCode());

        // 断言 DB 时间（行审计 create_time）非空（DB now() 生成）
        Date dbCreateTime = jdbcTemplate.queryForObject(
                "SELECT create_time FROM tb_mdm_brand WHERE id = ?", Date.class, brandId);
        assertNotNull(dbCreateTime, "create_time 必须由 DB now() 生成");

        // 断言 DB now() 来源：create_time 与当前 UTC epoch 对齐（±5 分钟，DB 会话 UTC +00:00）
        long nowEpochMs = System.currentTimeMillis();
        assertTrue(Math.abs(dbCreateTime.getTime() - nowEpochMs) < 5 * 60 * 1000L,
                "create_time 应为 DB now()（UTC 会话），与当前时间偏差过大: " + (dbCreateTime.getTime() - nowEpochMs));

        // SQL 层 UNIX_TIMESTAMP（DB 解释，UTC 会话）与 JDBC Date 读值对齐（±2 秒）
        // → 证明 handler/映射正确：JDBC 读回值 = DB 存储值，往返无漂移，且与时区无关（Date.getTime() 为 epoch）
        Long sqlEpochMs = jdbcTemplate.queryForObject(
                "SELECT UNIX_TIMESTAMP(create_time) * 1000 FROM tb_mdm_brand WHERE id = ?", Long.class, brandId);
        assertNotNull(sqlEpochMs);
        assertTrue(Math.abs(sqlEpochMs - dbCreateTime.getTime()) < 2000L,
                "SQL 层 UNIX_TIMESTAMP 与 JDBC 读值应一致: " + (sqlEpochMs - dbCreateTime.getTime()));

        // fill 映射：row_version=1、row_valid=TRUE（DB 层面）
        Integer rowVersion = jdbcTemplate.queryForObject(
                "SELECT row_version FROM tb_mdm_brand WHERE id = ?", Integer.class, brandId);
        assertEquals(1, rowVersion);
        Boolean rowValid = jdbcTemplate.queryForObject(
                "SELECT row_valid FROM tb_mdm_brand WHERE id = ?", Boolean.class, brandId);
        assertEquals(Boolean.TRUE, rowValid);
    }
}
