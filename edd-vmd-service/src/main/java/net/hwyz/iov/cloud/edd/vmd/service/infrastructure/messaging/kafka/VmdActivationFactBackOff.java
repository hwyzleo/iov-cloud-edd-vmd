package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.BackOffExecution;

/**
 * 首次激活事实消费退避策略（VMD-DSN-CR-077 / D-VMD-077-07）
 * <p>
 * 固定语义：总投递次数 = {@code maxAttempts}（含首次），退避间隔按
 * {@code initialInterval * multiplier^n} 指数增长并以 {@code maxInterval} 封顶，
 * 默认 {@code 3 次 / 100ms→1s}。
 * <p>
 * 与 {@code ExponentialBackOff} 的区别：此处按<strong>次数</strong>而非累计时长终止，
 * 便于「退避 3 次后进入 FAILED 终态」的确定性验收（TC-077-09）。
 *
 * @author hwyz_leo
 */
public class VmdActivationFactBackOff implements BackOff {

    private final int maxAttempts;
    private final long initialIntervalMs;
    private final double multiplier;
    private final long maxIntervalMs;

    /**
     * 构造退避策略
     *
     * @param maxAttempts       总投递次数（含首次，≥1）
     * @param initialIntervalMs 首次退避间隔（毫秒）
     * @param multiplier        退避倍数
     * @param maxIntervalMs     最大退避间隔（毫秒）
     */
    public VmdActivationFactBackOff(int maxAttempts, long initialIntervalMs, double multiplier, long maxIntervalMs) {
        this.maxAttempts = Math.max(1, maxAttempts);
        this.initialIntervalMs = Math.max(0L, initialIntervalMs);
        this.multiplier = multiplier <= 0 ? 1.0d : multiplier;
        this.maxIntervalMs = Math.max(this.initialIntervalMs, maxIntervalMs);
    }

    @Override
    public BackOffExecution start() {
        return new BackOffExecution() {
            private int attempt = 0;

            @Override
            public long nextBackOff() {
                attempt++;
                if (attempt >= maxAttempts) {
                    // 已用尽全部投递次数 → 停止重试，交由恢复器归档 FAILED 并提交位点
                    return BackOffExecution.STOP;
                }
                long interval = (long) (initialIntervalMs * Math.pow(multiplier, attempt - 1));
                return Math.min(interval, maxIntervalMs);
            }
        };
    }

    /**
     * 最大投递次数（含首次）
     *
     * @return 次数
     */
    public int maxAttempts() {
        return maxAttempts;
    }

}
