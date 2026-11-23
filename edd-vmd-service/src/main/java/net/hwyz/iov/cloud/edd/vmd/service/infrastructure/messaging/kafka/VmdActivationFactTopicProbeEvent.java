package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

/**
 * 首次激活事实消费 Topic 预检结果事件（VMD-DSN-CR-077 §8.2）
 * <p>
 * 用于解耦「探针 → 启动协调器」：探针每轮探测后发布，协调器据此决定是否启动容器
 * （门禁开启时）。Topic 缺失（{@code NOT_PRESENT}）不视为故障，不阻塞启动。
 *
 * @param state      探测状态
 * @param partitions 分区数
 * @author hwyz_leo
 */
public record VmdActivationFactTopicProbeEvent(VmdActivationFactTopicReadiness.State state, int partitions) {
}
