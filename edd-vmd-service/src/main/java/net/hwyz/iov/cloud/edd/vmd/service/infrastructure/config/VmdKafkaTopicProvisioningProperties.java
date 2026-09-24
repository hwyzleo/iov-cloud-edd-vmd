package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * VMD Kafka Topic 声明环境参数
 * <p>
 * 由 VmdKafkaTopicDefinitionProvider 读取并构造 KafkaTopicDefinition；
 * 分区数、副本数、cleanup.policy 等环境参数通过 Nacos 配置绑定覆盖。
 * <p>
 * VMD-DSN-CR-051：分区数、副本数及关键 Topic 参数来自环境化配置，
 * 代码仅提供开发环境默认值，生产环境必须显式配置。
 *
 * @author hwyz_leo
 */
@Data
@Component
@ConfigurationProperties(prefix = "vmd.kafka.topic-provisioning")
public class VmdKafkaTopicProvisioningProperties {

    /**
     * Topic 分区数
     */
    private int partitions = 3;

    /**
     * Topic 副本数（默认 1，适配单节点 broker；生产多副本环境经 Nacos 覆盖）
     */
    private short replicationFactor = 1;

    /**
     * Topic 清理策略（cleanup.policy），默认 delete
     */
    private String cleanupPolicy = "delete";

    /**
     * 构造 Topic 级配置（仅首次创建时使用；已存在 Topic 不执行 alterConfigs）
     *
     * @return Topic 级配置
     */
    public Map<String, String> configs() {
        Map<String, String> configs = new HashMap<>();
        if (cleanupPolicy != null && !cleanupPolicy.isBlank()) {
            configs.put("cleanup.policy", cleanupPolicy);
        }
        return configs;
    }
}
