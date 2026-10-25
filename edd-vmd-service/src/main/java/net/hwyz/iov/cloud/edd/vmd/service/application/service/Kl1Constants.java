package net.hwyz.iov.cloud.edd.vmd.service.application.service;

/**
 * KEYPROV-LIFECYCLE-1（KL1）规范常量（PAR-PROTO-DSN-CR-010 §8.6 / PAR-PROTO-REQ-CR-010 KL-REQ-001~007）。
 * <p>
 * 本组常量为协议层固定值，仅允许由 PAR-PROTO KL1 基线统一调整；VMD 不得单独改变默认。
 * 参数与 {@code verify/KeyprovLifecycleDecide.java}（参考模型）一致，但生产实现必须调用
 * VMD 自身决策函数（{@link Kl1LifecycleDecide}），不得调用参考 oracle 生成 actual。
 *
 * @author hwyz_leo
 * @since 2026-10-25
 */
public final class Kl1Constants {

    private Kl1Constants() {
    }

    /** KL1 profile 标识（批次 lifecycle_profile） */
    public static final String PROFILE = "KEYPROV-LIFECYCLE-1";

    /** 响应租约 TTL：E = min(server_now + 60s, B - 30s) */
    public static final long RESPONSE_TTL_MS = 60_000L;

    /** 批次 ACK 截止：B = F + 300s，不可滑动 */
    public static final long BATCH_MS = 300_000L;

    /** ACK 宽限：E = min(now + TTL, B - GRACE) */
    public static final long ACK_GRACE_MS = 30_000L;

    /** E - now < 20s → EXPIRED 无 items（不 SUCCEEDED） */
    public static final long ISSUE_MIN_MS = 20_000L;

    /** 设备导入准入：U < E 且 E - U ≥ 15s（TSP 侧 accept 用；VMD 只签发 E） */
    public static final long IMPORT_MIN_MS = 15_000L;

    /** 可信时间：样本不确定性 ≤ 1000ms */
    public static final long UNCERTAINTY_MAX_MS = 1_000L;

    /** 可信时间：样本新鲜度 ≤ 300s */
    public static final long SAMPLE_AGE_MAX_MS = 300_000L;

    /** 可信时间：车云偏差 ≤ 2s（等号通过） */
    public static final long SKEW_MS = 2_000L;

    /** ACK 窗口：D = E + 30s - 2s（TSP 侧）；VMD 以 B 为固定截止 */
    public static final long ACK_WINDOW_GRACE_MS = 30_000L;

    /** 服务端接收统计（非车端预算）：每 rid Request 参考上限 8 次 / 120s（VMD 只做接收统计或独立限流，不替代 TSP 预算） */
    public static final long REQUEST_LIMIT = 8L;
    public static final long REQUEST_WINDOW_MS = 120_000L;

    /** 服务端接收统计：每 rid ACK 参考上限 3 次 */
    public static final long ACK_LIMIT = 3L;

    /** 服务端接收统计：Status/SEC 只读参考上限 12 次 / 300s */
    public static final long READONLY_LIMIT = 12L;
    public static final long READONLY_WINDOW_MS = 300_000L;

    /** 恢复链 bound：最多 3 个 rid / 600s（VMD 侧拒绝抢占未释放批次，天然限制链） */
    public static final long CHAIN_IDS = 3L;
    public static final long CHAIN_WINDOW_MS = 600_000L;

    /** SEC proven 观测：同 epoch 连续 3 次强 NOT_FOUND、跨度 ≥ 12s（VMD 侧不实现 SEC 观测，保留常量供 TSP/SEC 对账） */
    public static final long SEC_NF_OBSERVATIONS = 3L;
    public static final long SEC_NF_SPAN_MS = 12_000L;

    /** 不可变内容摘要域分隔（KL-REQ-002）：immutable_content_digest = SHA256(ASCII(domain) || 0x00 || C) */
    public static final String CONTENT_DIGEST_DOMAIN = "openiov|keyprov-content|kl1";

    /** Provider 处理中超过该秒数且副作用 unknown：只隔离告警，不按可释放失败处理（VMD-DSN-CR-069 §4） */
    public static final long PROVIDER_INFLIGHT_UNKNOWN_SECONDS = 120L;

    /** 终态治理原因（terminal_reason；公开 Status 仍复用既有枚举，不臆造新枚举/错误码字段） */
    public static final String TERMINAL_REASON_ACK_TIMEOUT = "ACK_TIMEOUT";
    public static final String TERMINAL_REASON_MIGRATION_QUARANTINE = "QUARANTINE_MIGRATION";

    /** 外部公开错误：806095 仍为同 ID 内容冲突；806099 仍仅表示严格无批次（VMD-KL-REQ-004） */
    public static final String ERR_CONTENT_CONFLICT = "KEYPROV_IDEMPOTENCY_CONFLICT";
}
