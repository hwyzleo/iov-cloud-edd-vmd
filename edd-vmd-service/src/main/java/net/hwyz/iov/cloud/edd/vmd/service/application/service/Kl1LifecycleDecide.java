package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import java.time.Instant;

/**
 * KEYPROV-LIFECYCLE-1（KL1）VMD 生产决策纯函数（PAR-PROTO-DSN-CR-010 §8.6.2）。
 * <p>
 * TTL 签发、状态/错误矩阵、时间比较全部抽成纯函数：每个决策只依赖输入参数与固定常量，
 * 时间一律以参数传入（UTC epoch ms），不使用 wall clock / sleep / RNG / I/O。
 * 与 PAR-PROTO 参考模型 {@code KeyprovLifecycleDecide} 同语义；本类是可注入测试的生产实现，
 * 禁止在 conformance 测试中调用参考 oracle 生成 actual。
 * <p>
 * VMD 侧职责：issue（租约签发）、ack（ACK 线性化）、dedupe（内容幂等）、budget（服务端
 * 接收统计/独立限流，不替代车端预算）、expired（freshen≤1）、time（可信时间）、chain（拒绝
 * 抢占未释放批次）、store（重放存储 fail-closed）。accept/transport/stabilize/new_attempt/
 * sec_contract 属 TSP/SEC 职责，VMD 不实现（见 {@link Kl1Constants} 注释）。
 *
 * @author hwyz_leo
 * @since 2026-10-25
 */
public final class Kl1LifecycleDecide {

    private Kl1LifecycleDecide() {
    }

    /** LC01/LC03/LC20 — 租约签发：E = min(now + 60s, B - 30s)；E-now < 20s → EXPIRED_NO_ITEMS。 */
    public static IssueDecision issue(long nowMs, long batchDeadlineMs, boolean safetyValidThroughE) {
        if (!safetyValidThroughE) {
            return IssueDecision.securityBlocked();
        }
        long e = Math.min(nowMs + Kl1Constants.RESPONSE_TTL_MS, batchDeadlineMs - Kl1Constants.ACK_GRACE_MS);
        if (e - nowMs < Kl1Constants.ISSUE_MIN_MS) {
            return IssueDecision.expiredNoItems();
        }
        return IssueDecision.freshResponse(e);
    }

    /** LC04/LC05/LC19 — ACK 线性化：锁内先判已有 ACTIVE（精确重复 proof 幂等），再判 B 到期。 */
    public static AckDecision ack(String state, long nowMs, long batchDeadlineMs,
                                  boolean validDualProof, boolean exactRepeat) {
        if ("ACTIVE".equals(state)) {
            // ACTIVE 快速路径必须重新鉴权并精确匹配持久化 ACK 身份/两用途 proof 摘要；错 proof 拒绝
            return AckDecision.active(exactRepeat);
        }
        if ("FAILED".equals(state) || nowMs >= batchDeadlineMs) {
            // 到期未激活 → FAILED(ACK_TIMEOUT)；FAILED 迟到 ACK 绝不复活
            return AckDecision.failedAckTimeout();
        }
        if (!validDualProof) {
            return AckDecision.rejectAck();
        }
        return AckDecision.activate();
    }

    /** LC07/LC08 — 同 rid 内容幂等：业务内容摘要相同 → 新鲜租约；不同 → 806095 冲突，只读对账。 */
    public static DedupeDecision dedupe(boolean sameBusinessDigest) {
        return sameBusinessDigest ? DedupeDecision.sameBatchFreshLease() : DedupeDecision.conflict806095();
    }

    /** LC09 — 服务端接收统计预算（非车端预算）：Request 参考 8 次/120s，耗尽转只读。 */
    public static BudgetDecision budget(String phase, long count, long elapsedMs, boolean beforeD) {
        long n;
        long t;
        boolean unbounded = false;
        if ("REQUEST".equals(phase)) {
            n = Kl1Constants.REQUEST_LIMIT;
            t = Kl1Constants.REQUEST_WINDOW_MS;
        } else if ("ACK".equals(phase)) {
            n = Kl1Constants.ACK_LIMIT;
            t = Long.MAX_VALUE;
            unbounded = true;
        } else { // STATUS / SEC 只读
            n = Kl1Constants.READONLY_LIMIT;
            t = Kl1Constants.READONLY_WINDOW_MS;
        }
        boolean exhausted = count >= n || elapsedMs >= t || (unbounded && !beforeD);
        return exhausted ? BudgetDecision.blocked() : BudgetDecision.send();
    }

    /** LC16 — 同 rid 到期后最多一次 freshen；之后仅 Status。 */
    public static ExpiredDecision expired(boolean proven, boolean cloudAckPending, long freshenUsed,
                                          boolean deliverable, boolean requestBudgetOk) {
        boolean fresh = proven && cloudAckPending && freshenUsed == 0 && deliverable && requestBudgetOk;
        return fresh ? ExpiredDecision.freshenSameId() : ExpiredDecision.readonly();
    }

    /** LC21 — 可信时间门禁：uncertainty/sample_age/回拨/偏差任一越界 → TIME_UNTRUSTED（不得靠延长 TTL 放行）。 */
    public static TimeDecision time(long uncertaintyMs, long sampleAgeMs, boolean rollback, long skewMs) {
        boolean bad = uncertaintyMs > Kl1Constants.UNCERTAINTY_MAX_MS
                || sampleAgeMs > Kl1Constants.SAMPLE_AGE_MAX_MS
                || rollback
                || Math.abs(skewMs) > Kl1Constants.SKEW_MS;
        return bad ? TimeDecision.untrusted() : TimeDecision.ready();
    }

    /** LC22 — 恢复链 bound：已有 3 rid 或窗口耗尽 → BLOCKED（VMD 侧拒绝抢占未终结/未释放批次）。 */
    public static ChainDecision chain(long chainIds, long elapsedMs) {
        boolean blocked = chainIds >= Kl1Constants.CHAIN_IDS || elapsedMs >= Kl1Constants.CHAIN_WINDOW_MS;
        return blocked ? ChainDecision.blocked() : ChainDecision.allowSuccessor();
    }

    /** LC24 — 重放存储 fail-closed：digest 损坏/未知 codec/profile → 隔离，禁止重新 derive 补齐。 */
    public static StoreDecision store(boolean digestValid, boolean codecKnown, boolean profileKnown) {
        boolean ok = digestValid && codecKnown && profileKnown;
        return ok ? StoreDecision.replay() : StoreDecision.storeQuarantine();
    }

    // ---- 决策结果（纯数据，供上层映射为协议 action） ----

    public static final class IssueDecision {
        private final String action;
        private final long expireAtMs;

        private IssueDecision(String action, long expireAtMs) {
            this.action = action;
            this.expireAtMs = expireAtMs;
        }

        public static IssueDecision freshResponse(long expireAtMs) {
            return new IssueDecision("FRESH_RESPONSE", expireAtMs);
        }

        public static IssueDecision expiredNoItems() {
            return new IssueDecision("EXPIRED_NO_ITEMS", 0L);
        }

        public static IssueDecision securityBlocked() {
            return new IssueDecision("SECURITY_BLOCKED", 0L);
        }

        public String action() {
            return action;
        }

        public long expireAtMs() {
            return expireAtMs;
        }
    }

    public static final class AckDecision {
        private final String action;
        private final boolean shouldActivate;

        private AckDecision(String action, boolean shouldActivate) {
            this.action = action;
            this.shouldActivate = shouldActivate;
        }

        public static AckDecision active(boolean exactRepeat) {
            return new AckDecision(exactRepeat ? "ACTIVE" : "REJECT_ACK", false);
        }

        public static AckDecision failedAckTimeout() {
            return new AckDecision("FAILED_ACK_TIMEOUT", false);
        }

        public static AckDecision rejectAck() {
            return new AckDecision("REJECT_ACK", false);
        }

        public static AckDecision activate() {
            return new AckDecision("ACTIVE", true);
        }

        public String action() {
            return action;
        }

        public boolean shouldActivate() {
            return shouldActivate;
        }
    }

    public static final class DedupeDecision {
        private final String action;

        private DedupeDecision(String action) {
            this.action = action;
        }

        public static DedupeDecision sameBatchFreshLease() {
            return new DedupeDecision("SAME_BATCH_FRESH_LEASE");
        }

        public static DedupeDecision conflict806095() {
            return new DedupeDecision("CONFLICT_806095");
        }

        public String action() {
            return action;
        }
    }

    public static final class BudgetDecision {
        private final String action;

        private BudgetDecision(String action) {
            this.action = action;
        }

        public static BudgetDecision send() {
            return new BudgetDecision("SEND");
        }

        public static BudgetDecision blocked() {
            return new BudgetDecision("BLOCKED");
        }

        public String action() {
            return action;
        }
    }

    public static final class ExpiredDecision {
        private final String action;

        private ExpiredDecision(String action) {
            this.action = action;
        }

        public static ExpiredDecision freshenSameId() {
            return new ExpiredDecision("FRESHEN_SAME_ID");
        }

        public static ExpiredDecision readonly() {
            return new ExpiredDecision("READONLY");
        }

        public String action() {
            return action;
        }
    }

    public static final class TimeDecision {
        private final String action;

        private TimeDecision(String action) {
            this.action = action;
        }

        public static TimeDecision ready() {
            return new TimeDecision("TIME_READY");
        }

        public static TimeDecision untrusted() {
            return new TimeDecision("TIME_UNTRUSTED");
        }

        public String action() {
            return action;
        }
    }

    public static final class ChainDecision {
        private final String action;

        private ChainDecision(String action) {
            this.action = action;
        }

        public static ChainDecision allowSuccessor() {
            return new ChainDecision("ALLOW_SUCCESSOR");
        }

        public static ChainDecision blocked() {
            return new ChainDecision("BLOCKED");
        }

        public String action() {
            return action;
        }
    }

    public static final class StoreDecision {
        private final String action;

        private StoreDecision(String action) {
            this.action = action;
        }

        public static StoreDecision replay() {
            return new StoreDecision("REPLAY");
        }

        public static StoreDecision storeQuarantine() {
            return new StoreDecision("STORE_QUARANTINE");
        }

        public String action() {
            return action;
        }
    }

    /** 便捷：当前 UTC epoch ms（业务层注入，便于测试固定时间）。 */
    public static long nowMs(Instant now) {
        return now.toEpochMilli();
    }
}
