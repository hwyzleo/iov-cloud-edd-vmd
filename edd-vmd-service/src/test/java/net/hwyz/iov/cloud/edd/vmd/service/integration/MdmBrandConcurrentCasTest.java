package net.hwyz.iov.cloud.edd.vmd.service.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VMD-DSN-CR-075 T04 真实并发 CAS：两个独立连接读取同一版本，同步屏障并发更新
 *
 * <p>用两个独立 JDBC 连接（各开事务）读取同一 row_version，CountDownLatch 同步后同时执行
 * {@code UPDATE ... WHERE id=? AND row_version=1}。InnoDB 行锁保证仅一个成功：先获得锁的
 * 连接更新成功（1 行），另一连接等待其提交后重读 WHERE 不匹配 → 0 行。断言仅一个成功、
 * 版本只增一次（1→2）、失败方返回 0（不报告成功）。</p>
 *
 * <p>顺序执行只能证明过期版本被拒绝，不能代替并发验证——本测试为真实并发语义。</p>
 *
 * @author hwyz_leo
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
class MdmBrandConcurrentCasTest {

    @Autowired
    private DataSource dataSource;

    private Long brandId;

    @BeforeEach
    void setUp() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO tb_mdm_brand (code, name, enable, sort, source, create_time, create_by, modify_time, modify_by, row_version, row_valid) "
                            + "VALUES ('TEST_CONCURRENT_CAS', '并发CAS', 1, 1, 'MANUAL', now(), NULL, now(), NULL, 1, 1)")) {
                ps.executeUpdate();
            }
            conn.commit();
            try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM tb_mdm_brand WHERE code = 'TEST_CONCURRENT_CAS'")) {
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    brandId = rs.getLong(1);
                }
            }
        }
    }

    @Test
    @DisplayName("两个独立连接并发 CAS 更新：仅一个成功、版本只增一次、失败方返回 0")
    void concurrentCasUpdate_shouldAllowOnlyOneWinner() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>(2);

        for (int i = 0; i < 2; i++) {
            futures.add(executor.submit(() -> {
                try (Connection conn = dataSource.getConnection()) {
                    conn.setAutoCommit(false);
                    // 各连接独立读取同一版本
                    int version;
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT row_version FROM tb_mdm_brand WHERE id = ?")) {
                        ps.setLong(1, brandId);
                        try (ResultSet rs = ps.executeQuery()) {
                            assertTrue(rs.next());
                            version = rs.getInt(1);
                        }
                    }
                    assertEquals(1, version, "并发前两个连接都应读到版本 1");
                    ready.countDown();
                    start.await(10, TimeUnit.SECONDS); // 同步屏障：两个连接同时发起更新
                    // 同一 WHERE row_version=1 的 CAS 更新：一个连接先获锁更新成功，另一个等待后 0 行
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE tb_mdm_brand SET name = '并发CAS-已更新', row_version = row_version + 1, modify_time = now() "
                                    + "WHERE id = ? AND row_version = 1")) {
                        ps.setLong(1, brandId);
                        int rows = ps.executeUpdate();
                        conn.commit();
                        return rows;
                    }
                }
            }));
        }

        ready.await(10, TimeUnit.SECONDS);
        start.countDown();

        List<Integer> results = new ArrayList<>(2);
        for (Future<Integer> f : futures) {
            results.add(f.get(30, TimeUnit.SECONDS));
        }
        executor.shutdownNow();

        // 断言：恰好一个成功（1 行）、一个失败（0 行）；失败方不报告成功
        assertTrue(results.contains(1), "必须恰好一个连接成功，实际结果=" + results);
        assertTrue(results.contains(0), "并发第二个连接必须返回 0，实际结果=" + results);
        assertEquals(1, results.stream().filter(r -> r == 1).count(), "仅一个成功，实际结果=" + results);

        // 版本只增一次：1 → 2
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT row_version FROM tb_mdm_brand WHERE id = ?")) {
                ps.setLong(1, brandId);
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(2, rs.getInt(1), "版本应只从 1 递增到 2");
                }
            }
            // 清理测试数据
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM tb_mdm_brand WHERE id = ?")) {
                ps.setLong(1, brandId);
                ps.executeUpdate();
            }
            conn.commit();
        }
    }
}
