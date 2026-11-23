package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 车辆首次激活事实消费配置（VMD-DSN-CR-077 / D-VMD-077-07 / D-VMD-077-12）
 * <p>
 * 暗部署默认关闭（{@code enabled=false}），与 {@code VehicleSoftwareInventoryObservedConsumer}
 * 同风格；上线顺序：建表 → 部署（开关关）→ 观察预检/指标 → 开开关（新消费组
 * {@code auto-offset-reset=earliest}，<strong>回放历史事实</strong>）→ 对账 → 如需切回 {@code latest}。
 * <p>
 * <strong>终审 R-8（起始位点策略）</strong>：默认 {@code earliest}。原 {@code latest} 在首次开启消费时
 * 跳过 Topic 中既有事实，导致「早期已激活、事实在开启前已投递」的车辆永远缺首次激活节点
 * （查询恒为 {@code NOT_RECORDED}）。{@code earliest} 只对<strong>新消费组首次分配分区</strong>生效，
 * 已有 committed offset 的消费组不受影响（不会重复回放）；回放完成（lag 归零、台账无新增
 * {@code RECORDED}）后，如需仅消费增量可经环境变量
 * {@code VMD_ACTIVATION_FACT_KAFKA_AUTO_OFFSET_RESET=latest} 切回，无需改代码。
 * <p>
 * 失败处理：技术错误按 {@code retry.*} 退避重试，耗尽后台账 {@code FAILED} + 提交位点
 * （不毒化分区），<strong>不建 DLQ Topic</strong>（R-18 #6），按位点人工重放。
 *
 * @author hwyz_leo
 */
@Data
@Component
@ConfigurationProperties(prefix = "vmd.activation-fact.kafka")
public class VmdActivationFactConsumerProperties {

    /**
     * 消费开关（默认 false，暗部署）
     */
    private boolean enabled = false;

    /**
     * 独立消费组 ID
     */
    private String groupId = "edd-vmd-vehicle-activation-fact";

    /**
     * 新消费组起始位点（终审 R-8 定稿：{@code earliest}，开启前回放历史事实，
     * 补齐早期已激活车辆的首次激活节点；仅新消费组首次生效，已有位点不受影响；
     * 回放完成后如需可切回 {@code latest} 仅消费增量）
     */
    private String autoOffsetReset = "earliest";

    /**
     * Listener 容器是否由 Spring 自动启动（默认 false：由启动协调器按门禁启动，
     * Topic 缺失不阻塞启动、就绪后自恢复）
     */
    private boolean autoStartup = false;

    /**
     * 启动门禁：true 时仅当 Topic 预检 UP 才启动容器（默认 false=容错启动，
     * 由 Kafka 客户端在 Topic 就绪后自动接续消费）
     */
    private boolean startupGateEnabled = false;

    /**
     * Topic 预检失败后的后台重试探测间隔（毫秒）
     */
    private long topicProbeRetryIntervalMs = 60_000L;

    /**
     * 退避重试参数（技术错误）
     */
    private Retry retry = new Retry();

    /**
     * 退避重试参数。
     */
    @Data
    public static class Retry {

        /**
         * 最大尝试次数（含首次，默认 3）
         */
        private int maxAttempts = 3;

        /**
         * 首次退避间隔（毫秒，默认 100）
         */
        private long initialIntervalMs = 100L;

        /**
         * 退避倍数（默认 2.0，指数退避）
         */
        private double multiplier = 2.0d;

        /**
         * 最大退避间隔（毫秒，默认 1000）
         */
        private long maxIntervalMs = 1_000L;
    }

}
