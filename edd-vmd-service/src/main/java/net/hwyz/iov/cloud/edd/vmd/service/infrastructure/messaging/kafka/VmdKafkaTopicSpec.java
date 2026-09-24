package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

import java.util.Map;

/**
 * VMD Kafka Topic 初始化规格
 * <p>
 * VMD-DSN-CR-051：Topic 初始化组件的输入模型，
 * 描述逻辑名、实际 Topic、所有权、分区数、副本数与关键配置。
 *
 * @param logicalName      逻辑名（Kafka Topic 目录）
 * @param topicName        实际 Topic 名称
 * @param ownership        所有权（PRODUCER_OWNED 由 VMD 创建；CONSUMER_ONLY 不创建）
 * @param partitions       分区数（环境化配置，代码提供开发默认值）
 * @param replicationFactor 副本数（环境化配置，代码提供开发默认值）
 * @param configs          Topic 级配置（如 cleanup.policy，仅首次创建使用）
 * @author hwyz_leo
 */
public record VmdKafkaTopicSpec(
        VmdKafkaLogicalTopic logicalName,
        String topicName,
        VmdKafkaTopicRole ownership,
        int partitions,
        short replicationFactor,
        Map<String, String> configs) {

    public VmdKafkaTopicSpec {
        configs = configs == null ? Map.of() : Map.copyOf(configs);
    }
}
