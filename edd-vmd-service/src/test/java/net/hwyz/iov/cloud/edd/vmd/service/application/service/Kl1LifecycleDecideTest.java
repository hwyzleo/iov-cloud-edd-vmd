package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link Kl1LifecycleDecide} 纯函数决策单元测试（PAR-PROTO-DSN-CR-010 §8.6.3，固定 T0）。
 * <p>
 * 覆盖 VMD 侧适用子例：LC01/LC03/LC04/LC05/LC07/LC08/LC09/LC16/LC19/LC20/LC21/LC22/LC24；
 * T0=1800000000000ms 固定，绝不用执行机器时间。accept/transport/stabilize/new_attempt/sec_contract
 * 属 TSP/SEC 职责，VMD 不实现。
 *
 * @author hwyz_leo
 * @since 2026-10-25
 */
class Kl1LifecycleDecideTest {

    private static final long T0 = 1_800_000_000_000L;

    // ---- LC01 / LC03 / LC20 issue ----

    @Test
    @DisplayName("LC01：首次签发 E=min(now+60s, B-30s)；F=T0,B=T0+300s,now=T0+90s → E=T0+150s")
    void issueLc01() {
        Kl1LifecycleDecide.IssueDecision d = Kl1LifecycleDecide.issue(T0 + 90_000L, T0 + 300_000L, true);
        assertEquals("FRESH_RESPONSE", d.action());
        assertEquals(T0 + 150_000L, d.expireAtMs(), "E=min(now+60s=150s, B-30s=270s)=150s");
    }

    @Test
    @DisplayName("LC03：now=T0+255s 候选 E-now=15s<20s → EXPIRED_NO_ITEMS（无 items，不 SUCCEEDED）")
    void issueLc03() {
        Kl1LifecycleDecide.IssueDecision d = Kl1LifecycleDecide.issue(T0 + 255_000L, T0 + 300_000L, true);
        assertEquals("EXPIRED_NO_ITEMS", d.action());
        assertEquals(0L, d.expireAtMs());
    }

    @Test
    @DisplayName("LC20：安全期限不覆盖拟签发 E → SECURITY_BLOCKED，不人为延密钥期限")
    void issueLc20() {
        Kl1LifecycleDecide.IssueDecision d = Kl1LifecycleDecide.issue(T0 + 90_000L, T0 + 300_000L, false);
        assertEquals("SECURITY_BLOCKED", d.action());
    }

    // ---- LC04 / LC05 / LC19 ack ----

    @Test
    @DisplayName("LC04：now=B-1 双 proof 合法 → ACTIVE 激活；now=B → FAILED(ACK_TIMEOUT)")
    void ackLc04() {
        Kl1LifecycleDecide.AckDecision before = Kl1LifecycleDecide.ack("ACK_PENDING", T0 + 299_999L, T0 + 300_000L, true, false);
        assertEquals("ACTIVE", before.action());
        assertTrue(before.shouldActivate(), "B 前双 proof 合法 → 原子激活");

        Kl1LifecycleDecide.AckDecision at = Kl1LifecycleDecide.ack("ACK_PENDING", T0 + 300_000L, T0 + 300_000L, true, false);
        assertEquals("FAILED_ACK_TIMEOUT", at.action(), "now≥B 未激活 → ACK_TIMEOUT");
    }

    @Test
    @DisplayName("LC05：已 ACTIVE，原 ACK 重发 → 精确重复 proof 幂等 ACTIVE；错 proof → REJECT_ACK；不重复完成事件")
    void ackLc05() {
        Kl1LifecycleDecide.AckDecision ok = Kl1LifecycleDecide.ack("ACTIVE", T0 + 400_000L, T0 + 300_000L, true, true);
        assertEquals("ACTIVE", ok.action());
        Kl1LifecycleDecide.AckDecision wrong = Kl1LifecycleDecide.ack("ACTIVE", T0 + 400_000L, T0 + 300_000L, true, false);
        assertEquals("REJECT_ACK", wrong.action());
    }

    @Test
    @DisplayName("LC19：FAILED 状态迟到 ACK 绝不复活（含跨 B 锁内判定）")
    void ackLc19() {
        Kl1LifecycleDecide.AckDecision d = Kl1LifecycleDecide.ack("FAILED", T0 + 400_000L, T0 + 300_000L, true, true);
        assertEquals("FAILED_ACK_TIMEOUT", d.action());
        assertFalse(d.shouldActivate());
    }

    // ---- LC07 / LC08 dedupe ----

    @Test
    @DisplayName("LC07：同 requestId 同业务内容 → SAME_BATCH_FRESH_LEASE；业务版本不增加")
    void dedupeLc07() {
        assertEquals("SAME_BATCH_FRESH_LEASE", Kl1LifecycleDecide.dedupe(true).action());
    }

    @Test
    @DisplayName("LC08：同 requestId 证书/用途/currentVersions 变 → CONFLICT_806095（只读对账，derive=0）")
    void dedupeLc08() {
        assertEquals("CONFLICT_806095", Kl1LifecycleDecide.dedupe(false).action());
    }

    // ---- LC09 budget（VMD 服务端接收统计，不替代车端预算） ----

    @Test
    @DisplayName("LC09：Request 计数 8 或窗口 120s 耗尽 → BLOCKED；重启/重连不重置（计数来自持久化）")
    void budgetLc09() {
        assertEquals("SEND", Kl1LifecycleDecide.budget("REQUEST", 7, 60_000L, true).action());
        assertEquals("BLOCKED", Kl1LifecycleDecide.budget("REQUEST", 8, 60_000L, true).action());
        assertEquals("BLOCKED", Kl1LifecycleDecide.budget("REQUEST", 3, 120_000L, true).action());
        assertEquals("BLOCKED", Kl1LifecycleDecide.budget("ACK", 3, 0L, true).action());
        assertEquals("BLOCKED", Kl1LifecycleDecide.budget("ACK", 1, 0L, false).action(), "D 后 ACK 只读");
    }

    // ---- LC16 expired（freshen ≤ 1） ----

    @Test
    @DisplayName("LC16：首次过期 proven+ACK_PENDING+freshen=0 → FRESHEN_SAME_ID；再次过期 → READONLY（仅 Status，不新 ID）")
    void expiredLc16() {
        assertEquals("FRESHEN_SAME_ID", Kl1LifecycleDecide.expired(true, true, 0, true, true).action());
        assertEquals("READONLY", Kl1LifecycleDecide.expired(true, true, 1, true, true).action());
        assertEquals("READONLY", Kl1LifecycleDecide.expired(false, true, 0, true, true).action());
        assertEquals("READONLY", Kl1LifecycleDecide.expired(true, false, 0, true, true).action());
    }

    // ---- LC21 time（等号边界） ----

    @Test
    @DisplayName("LC21：uncertainty=1000 等号通过；1001 越界 → TIME_UNTRUSTED；skew 等号 2000 通过")
    void timeLc21() {
        assertEquals("TIME_READY", Kl1LifecycleDecide.time(1_000L, 300_000L, false, 2_000L).action());
        assertEquals("TIME_UNTRUSTED", Kl1LifecycleDecide.time(1_001L, 300_000L, false, 0L).action());
        assertEquals("TIME_UNTRUSTED", Kl1LifecycleDecide.time(0L, 300_001L, false, 0L).action());
        assertEquals("TIME_UNTRUSTED", Kl1LifecycleDecide.time(0L, 0L, true, 0L).action());
        assertEquals("TIME_UNTRUSTED", Kl1LifecycleDecide.time(0L, 0L, false, 2_001L).action());
    }

    // ---- LC22 chain ----

    @Test
    @DisplayName("LC22：已有 3 rid 或窗口 600s 耗尽 → BLOCKED；反复重启不重置")
    void chainLc22() {
        assertEquals("ALLOW_SUCCESSOR", Kl1LifecycleDecide.chain(2, 599_999L).action());
        assertEquals("BLOCKED", Kl1LifecycleDecide.chain(3, 0L).action());
        assertEquals("BLOCKED", Kl1LifecycleDecide.chain(2, 600_000L).action());
    }

    // ---- LC24 store ----

    @Test
    @DisplayName("LC24：payload/digest 篡改或未知 codec/profile → STORE_QUARANTINE，禁重新 derive 补齐")
    void storeLc24() {
        assertEquals("REPLAY", Kl1LifecycleDecide.store(true, true, true).action());
        assertEquals("STORE_QUARANTINE", Kl1LifecycleDecide.store(false, true, true).action());
        assertEquals("STORE_QUARANTINE", Kl1LifecycleDecide.store(true, false, true).action());
        assertEquals("STORE_QUARANTINE", Kl1LifecycleDecide.store(true, true, false).action());
    }
}
