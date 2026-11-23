package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 车辆首次激活事实消费 Topic / 容器就绪状态（VMD-DSN-CR-077 §8.2）
 * <p>
 * 与 MDM 投影 Readiness 解耦（不进入 MDM Bootstrap 基线判定，D-VMD-077-02）：
 * <ul>
 *   <li>{@link State#NOT_PRESENT}：上游 Topic 暂不存在（E2E 常见）→ <strong>不阻塞启动</strong>，
 *       消费者容器照常启动由 Kafka 客户端在 Topic 就绪后接续消费；后台探针持续重试</li>
 *   <li>{@link State#DOWN}：ACL 拒绝 / Broker 不可达等真实故障，健康项报 DOWN</li>
 * </ul>
 *
 * @author hwyz_leo
 */
@Component
public class VmdActivationFactTopicReadiness {

    /**
     * 就绪状态。
     */
    public enum State {
        /**
         * 尚未完成预检评估。
         */
        INIT,
        /**
         * Topic 存在且可访问。
         */
        UP,
        /**
         * Topic 暂不存在（上游未就绪，VMD 不创建）。
         */
        NOT_PRESENT,
        /**
         * 探测失败（ACL / Broker / 超时）。
         */
        DOWN
    }

    private final AtomicReference<State> state = new AtomicReference<>(State.INIT);
    private final AtomicReference<String> reason = new AtomicReference<>();
    private final AtomicReference<Integer> partitions = new AtomicReference<>(0);
    private final AtomicBoolean consumerRunning = new AtomicBoolean(false);

    /**
     * 当前状态。
     *
     * @return 状态
     */
    public State state() {
        return state.get();
    }

    /**
     * 状态原因（失败/缺失原因；成功时为 null）。
     *
     * @return 原因
     */
    public String reason() {
        return reason.get();
    }

    /**
     * Topic 分区数（-1/0 表示未知）。
     *
     * @return 分区数
     */
    public int partitions() {
        return partitions.get() == null ? 0 : partitions.get();
    }

    /**
     * Listener 容器是否运行中。
     *
     * @return true=运行中
     */
    public boolean consumerRunning() {
        return consumerRunning.get();
    }

    /**
     * 标记 Topic 可访问。
     *
     * @param partitionCount 分区数
     */
    public void markUp(int partitionCount) {
        state.set(State.UP);
        reason.set(null);
        partitions.set(partitionCount);
    }

    /**
     * 标记 Topic 暂不存在（上游未就绪）。
     *
     * @param detail 原因
     */
    public void markNotPresent(String detail) {
        state.set(State.NOT_PRESENT);
        reason.set(detail);
        partitions.set(0);
    }

    /**
     * 标记探测失败。
     *
     * @param detail 原因
     */
    public void markDown(String detail) {
        state.set(State.DOWN);
        reason.set(detail);
    }

    /**
     * 标记容器运行状态。
     *
     * @param running true=运行中
     */
    public void markConsumerRunning(boolean running) {
        consumerRunning.set(running);
    }

}
