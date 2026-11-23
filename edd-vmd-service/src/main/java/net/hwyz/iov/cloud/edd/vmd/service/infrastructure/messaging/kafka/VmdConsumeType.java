package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

/**
 * VMD 自有消费类型登记（VMD-DSN-CR-077 / D-VMD-077-01，R-18 #3 定稿）
 * <p>
 * 与 {@link MdmProjectionType} <strong>并列而不混入</strong>：{@code MdmProjectionType} 语义已被
 * CR-052 固化为「11 个 EDD-MDM 投影」，把 TSP Topic 塞入会造成语义污染与 MDM 预检误报。
 * 本枚举登记 VMD 自有（非 MDM 投影）的消费 Topic，全部 {@link VmdKafkaTopicRole#CONSUMER_ONLY}：
 * 生命周期归上游生产者，VMD 只预检可访问性并消费，绝不创建、不改写。
 * <p>
 * 值来自类型化配置 {@code vmd.kafka.topics.<configKey>}；{@link ConsumerIds} 为 Listener 容器 id
 * 常量表，供 {@code @KafkaListener(id=...)} 编译期引用，保证容器 id 与注册表、启动协调器一致。
 *
 * @author hwyz_leo
 */
public enum VmdConsumeType {

    /**
     * IOV-TSP 车辆首次激活候选事实（Key=VIN，schemaVersion=1）
     */
    USER_VEHICLE_ACTIVATION_FACT(
            "user-vehicle-activation-fact",
            "tsp.user-vehicle-activation-fact",
            ConsumerIds.USER_VEHICLE_ACTIVATION_FACT,
            VmdKafkaLogicalTopic.USER_VEHICLE_ACTIVATION_FACT,
            VmdKafkaTopicRole.CONSUMER_ONLY);

    /**
     * VMD 自有消费 Listener 容器 id 常量表（供 @KafkaListener(id=...) 编译期引用）。
     */
    public interface ConsumerIds {
        /**
         * 车辆首次激活候选事实 Listener 容器 id
         */
        String USER_VEHICLE_ACTIVATION_FACT = "user-vehicle-activation-fact-consumer";
    }

    private final String configKey;
    private final String directoryTopic;
    private final String consumerId;
    private final VmdKafkaLogicalTopic logicalTopic;
    private final VmdKafkaTopicRole role;

    VmdConsumeType(String configKey, String directoryTopic, String consumerId,
                   VmdKafkaLogicalTopic logicalTopic, VmdKafkaTopicRole role) {
        this.configKey = configKey;
        this.directoryTopic = directoryTopic;
        this.consumerId = consumerId;
        this.logicalTopic = logicalTopic;
        this.role = role;
    }

    /**
     * 配置键（{@code vmd.kafka.topics.*} 下的段名）。
     */
    public String configKey() {
        return configKey;
    }

    /**
     * Kafka Topic 目录标准值（代码默认值；生产经 Nacos 覆盖，逐字符一致由配置测试固化）。
     */
    public String directoryTopic() {
        return directoryTopic;
    }

    /**
     * 对应 @KafkaListener 的容器 id（供启动协调器按消费类型启停容器）。
     */
    public String consumerId() {
        return consumerId;
    }

    /**
     * 对应的逻辑 Topic（Topic 目录 SSOT 条目）。
     */
    public VmdKafkaLogicalTopic logicalTopic() {
        return logicalTopic;
    }

    /**
     * Topic 角色（本枚举恒为 CONSUMER_ONLY）。
     */
    public VmdKafkaTopicRole role() {
        return role;
    }

}
