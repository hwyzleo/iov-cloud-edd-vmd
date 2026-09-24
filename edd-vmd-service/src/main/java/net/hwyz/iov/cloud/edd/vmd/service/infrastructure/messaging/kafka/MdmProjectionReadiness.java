package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * MDM 投影 Readiness 状态（VMD-DSN-CR-052，RD-052-4）
 * <p>
 * 按投影类型（11 个 MDM 消费 Topic）独立维护健康状态：
 * Topic 可访问性（topicReachable）、ACL（aclReady）、Bootstrap 基线（bootstrapStatus）、
 * Listener 运行（consumerRunning）、Lag 与最后成功消费时间（lastEventAt）分开暴露，
 * 避免 metadata 正常但 Bootstrap/消费未完成时误报健康（§5 约束 4）。
 * <p>
 * 预检失败时按投影置 DOWN（生产默认 fail-fast）；降级启动（fail-fast=false）时置
 * DEGRADED，健康检查仍不得掩蔽单投影故障。
 *
 * @author hwyz_leo
 */
@Component
public class MdmProjectionReadiness {

    /**
     * 就绪状态。
     */
    public enum State {
        /**
         * 尚未完成预检评估。
         */
        INIT,
        /**
         * Topic 可访问 + ACL 通过（Bootstrap/消费状态另行暴露）。
         */
        UP,
        /**
         * 预检失败（缺失 / 无权限 / Broker 异常）。
         */
        DOWN,
        /**
         * 降级启动（fail-fast=false），健康仍视为不可用。
         */
        DEGRADED
    }

    /**
     * Bootstrap 基线状态。
     */
    public enum BootstrapStatus {
        /**
         * 尚未执行 / 本地已有数据跳过。
         */
        NOT_RUN,
        /**
         * 执行中。
         */
        RUNNING,
        /**
         * 基线建立完成。
         */
        COMPLETED,
        /**
         * 基线失败（健康不得标记为已同步）。
         */
        FAILED
    }

    /**
     * 单投影状态快照（健康检查明细输出）。
     *
     * @param projection      投影类型
     * @param topic           实际 Topic
     * @param state           预检状态
     * @param topicReachable  Topic 是否存在且 Describe 通过
     * @param aclReady        Read / ConsumerGroup 权限是否通过
     * @param bootstrapStatus Bootstrap 基线状态
     * @param consumerRunning Listener 是否运行
     * @param lag             消费 Lag（-1 表示未知）
     * @param lastEventAt     最后成功消费时间（null 表示尚无）
     * @param reason          失败原因（不记录凭证）
     */
    public record ProjectionStatus(
            MdmProjectionType projection,
            String topic,
            State state,
            boolean topicReachable,
            boolean aclReady,
            BootstrapStatus bootstrapStatus,
            boolean consumerRunning,
            long lag,
            Instant lastEventAt,
            String reason) {
    }

    private final Map<MdmProjectionType, Holder> statuses = new EnumMap<>(MdmProjectionType.class);

    public MdmProjectionReadiness() {
        for (MdmProjectionType projection : MdmProjectionType.values()) {
            statuses.put(projection, new Holder(projection));
        }
    }

    private static final class Holder {
        final MdmProjectionType projection;
        String topic;
        volatile State state = State.INIT;
        volatile boolean topicReachable;
        volatile boolean aclReady;
        volatile BootstrapStatus bootstrapStatus = BootstrapStatus.NOT_RUN;
        volatile boolean consumerRunning;
        volatile long lag = -1;
        volatile Instant lastEventAt;
        volatile String reason;

        Holder(MdmProjectionType projection) {
            this.projection = projection;
            this.topic = projection.directoryTopic();
        }
    }

    /**
     * 预检通过（Topic 可访问 + ACL 通过）。
     */
    public void markPreflightPassed(MdmProjectionType projection, String topic) {
        Holder holder = statuses.get(projection);
        holder.topicReachable = true;
        holder.aclReady = true;
        holder.reason = null;
        holder.state = State.UP;
        // topic 以实际配置为准
        updateTopic(holder, topic);
    }

    /**
     * 预检失败（缺失 / 无权限 / Broker 异常）。
     */
    public void markPreflightFailed(MdmProjectionType projection, String topic, String reason) {
        Holder holder = statuses.get(projection);
        holder.topicReachable = false;
        holder.aclReady = false;
        holder.reason = reason;
        holder.state = State.DOWN;
        updateTopic(holder, topic);
    }

    /**
     * 降级启动（fail-fast=false 时由预检组件调用）。
     */
    public void markDegraded(MdmProjectionType projection, String topic, String reason) {
        Holder holder = statuses.get(projection);
        holder.topicReachable = false;
        holder.aclReady = false;
        holder.reason = reason;
        holder.state = State.DEGRADED;
        updateTopic(holder, topic);
    }

    /**
     * 记录 Bootstrap 基线状态。
     */
    public void markBootstrap(MdmProjectionType projection, BootstrapStatus status) {
        statuses.get(projection).bootstrapStatus = status;
    }

    /**
     * 记录 Listener 运行状态。
     */
    public void markConsumerRunning(MdmProjectionType projection, boolean running) {
        statuses.get(projection).consumerRunning = running;
    }

    /**
     * 记录消费 Lag（-1 表示未知）。
     */
    public void recordLag(MdmProjectionType projection, long lag) {
        statuses.get(projection).lag = lag;
    }

    /**
     * 记录最后成功消费时间。
     */
    public void recordEvent(MdmProjectionType projection, Instant at) {
        statuses.get(projection).lastEventAt = at;
    }

    /**
     * 重置为初始状态（后台重试前使用）。
     */
    public void reset() {
        for (Holder holder : statuses.values()) {
            holder.state = State.INIT;
            holder.topicReachable = false;
            holder.aclReady = false;
            holder.bootstrapStatus = BootstrapStatus.NOT_RUN;
            holder.consumerRunning = false;
            holder.lag = -1;
            holder.lastEventAt = null;
            holder.reason = null;
        }
    }

    /**
     * 单投影状态快照。
     */
    public ProjectionStatus status(MdmProjectionType projection) {
        Holder holder = statuses.get(projection);
        return new ProjectionStatus(holder.projection, holder.topic, holder.state,
                holder.topicReachable, holder.aclReady, holder.bootstrapStatus,
                holder.consumerRunning, holder.lag, holder.lastEventAt, holder.reason);
    }

    /**
     * 全部投影状态快照（顺序稳定，按枚举序）。
     */
    public List<ProjectionStatus> statuses() {
        List<ProjectionStatus> list = new ArrayList<>(statuses.size());
        for (MdmProjectionType projection : MdmProjectionType.values()) {
            list.add(status(projection));
        }
        return list;
    }

    /**
     * 全部投影预检通过。
     */
    public boolean allReady() {
        return statuses.values().stream().allMatch(h -> h.state == State.UP);
    }

    /**
     * 全部投影仍为 INIT（预检从未执行，如 Topic Provisioning 未启用）。
     */
    public boolean allInit() {
        return statuses.values().stream().allMatch(h -> h.state == State.INIT);
    }

    /**
     * 是否存在任一投影 DOWN / DEGRADED（健康检查用）。
     */
    public boolean anyFailure() {
        return statuses.values().stream()
                .anyMatch(h -> h.state == State.DOWN || h.state == State.DEGRADED);
    }

    private void updateTopic(Holder holder, String topic) {
        // topic 以实际配置为准；避免构造期默认值被覆盖为目录名时丢失（默认值本身即目录值）
        if (topic != null && !topic.isBlank()) {
            holder.topic = topic;
        }
    }
}
