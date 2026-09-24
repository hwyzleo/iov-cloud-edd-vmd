package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

/**
 * MDM 消费 Topic 预检规格（VMD-DSN-CR-052）
 * <p>
 * 描述 MDM 消费 Topic 的逻辑名、实际名称、投影类型、必需性与消费组。
 * 11 个 MDM Topic 全部为 CONSUMER_ONLY（所有权归 EDD-MDM）：
 * 该规格只进入 describe/metadata 与 ACL 预检，
 * 永不进入 createTopics / createPartitions / alterConfigs 集合（RD-052-2）。
 *
 * @param logicalName    逻辑名（Kafka Topic 目录）
 * @param topicName      实际 Topic 名称（来自 {@link VmdKafkaTopicProperties}）
 * @param projectionType 投影类型（显式注册表语义键）
 * @param required       是否必需（缺失/不可访问时置投影 readiness=DOWN）
 * @param consumerGroup  消费组（沿用既有稳定业务组名）
 * @param ownership      所有权（恒为 CONSUMER_ONLY，VMD 不创建）
 * @author hwyz_leo
 */
public record MdmConsumerTopicSpec(
        String logicalName,
        String topicName,
        MdmProjectionType projectionType,
        boolean required,
        String consumerGroup,
        VmdKafkaTopicRole ownership) {

    public MdmConsumerTopicSpec {
        if (ownership != VmdKafkaTopicRole.CONSUMER_ONLY) {
            throw new IllegalArgumentException(
                    "MDM 消费 Topic 必须是 CONSUMER_ONLY: " + logicalName + " -> " + topicName);
        }
    }
}
