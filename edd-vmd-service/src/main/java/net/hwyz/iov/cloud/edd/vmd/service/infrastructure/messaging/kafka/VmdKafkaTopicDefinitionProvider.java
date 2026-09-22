package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.VmdKafkaTopicProvisioningProperties;
import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicDefinition;
import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicDefinitionProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * VMD 下游 Kafka Topic 定义提供者
 * <p>
 * 向 FW-KAFKA 声明全部 VMD 作为生产者输出的 Topic，由框架统一完成
 * Catalog 合并、存在性检查、幂等创建、后台重试与状态传播：
 * <ul>
 *   <li>{@code vmd-vehicle-binding-changed}：车辆-零件绑定变更事件，
 *       由 {@code VehiclePartBindingKafkaProducer} 直发（topic 名与生产者配置保持一致）</li>
 *   <li>{@code vmd-vehicle-software-inventory-changed}：车辆软件清单变更事件，
 *       经 vmd_outbox → OutboxRelay 发布</li>
 * </ul>
 * <p>
 * VMD Provider 不声明 upstream.*（mdm.*、ota.* 等消费 Topic）或 DLQ Topic；
 * Topic 分区数、副本数通过 {@link VmdKafkaTopicProvisioningProperties} 环境参数注入。
 *
 * @author hwyz_leo
 */
@Component
@RequiredArgsConstructor
public class VmdKafkaTopicDefinitionProvider implements KafkaTopicDefinitionProvider {

    private final VmdKafkaTopicProvisioningProperties properties;

    /**
     * 车辆-零件绑定变更事件 Topic（与 {@code VehiclePartBindingKafkaProducer} 的 vmd.binding.kafka.topic 一致）
     */
    @Value("${vmd.binding.kafka.topic:vmd-vehicle-binding-changed}")
    private String bindingChangedTopic;

    /**
     * 车辆软件清单变更事件 Topic（与 {@code SoftwareInventoryAppService} 的 vmd.software-inventory.changed.kafka.topic 一致）
     */
    @Value("${vmd.software-inventory.changed.kafka.topic:vmd-vehicle-software-inventory-changed}")
    private String softwareInventoryChangedTopic;

    @Override
    public Collection<KafkaTopicDefinition> topicDefinitions() {
        List<KafkaTopicDefinition> definitions = new ArrayList<>(2);
        definitions.add(new KafkaTopicDefinition(
                bindingChangedTopic, properties.getPartitions(), properties.getReplicationFactor()));
        definitions.add(new KafkaTopicDefinition(
                softwareInventoryChangedTopic, properties.getPartitions(), properties.getReplicationFactor()));
        return definitions;
    }
}
