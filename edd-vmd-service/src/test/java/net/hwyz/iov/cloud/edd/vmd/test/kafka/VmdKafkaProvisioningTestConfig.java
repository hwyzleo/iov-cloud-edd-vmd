package net.hwyz.iov.cloud.edd.vmd.test.kafka;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.VmdKafkaTopicProperties;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.VmdKafkaTopicProvisioningProperties;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdKafkaTopicDefinitionProvider;
import net.hwyz.iov.cloud.framework.kafka.autoconfigure.KafkaAutoConfiguration;
import net.hwyz.iov.cloud.framework.kafka.autoconfigure.KafkaTopicProvisioningAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * VMD Kafka Topic Provisioning 测试装配
 * <p>
 * 仅装配 FW-KAFKA Topic Provisioning 自动配置 + VMD Kafka Bean，
 * 不加载 Nacos / MyBatis / Web 等业务自动配置，聚焦 Kafka 行为。
 *
 * @author hwyz_leo
 */
@SpringBootConfiguration
@EnableConfigurationProperties({KafkaProperties.class, VmdKafkaTopicProvisioningProperties.class, VmdKafkaTopicProperties.class})
@ImportAutoConfiguration({
        KafkaAutoConfiguration.class,
        KafkaTopicProvisioningAutoConfiguration.class
})
public class VmdKafkaProvisioningTestConfig {

    @Bean
    VmdKafkaTopicDefinitionProvider vmdKafkaTopicDefinitionProvider(
            VmdKafkaTopicProvisioningProperties properties,
            VmdKafkaTopicProperties topicProperties) {
        return new VmdKafkaTopicDefinitionProvider(properties, topicProperties);
    }
}
