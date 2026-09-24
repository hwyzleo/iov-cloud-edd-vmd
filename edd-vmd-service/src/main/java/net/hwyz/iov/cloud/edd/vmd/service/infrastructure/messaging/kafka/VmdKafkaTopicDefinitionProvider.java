package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.VmdKafkaTopicProperties;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.VmdKafkaTopicProvisioningProperties;
import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicDefinition;
import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicDefinitionProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * VMD 下游 Kafka Topic 定义提供者（VMD-DSN-CR-051）
 * <p>
 * 向 FW-KAFKA 声明 VMD 作为生产者输出的三个标准 Topic（PRODUCER_OWNED），
 * 由框架统一完成 Catalog 合并、存在性检查、幂等创建、后台重试与状态传播：
 * <ul>
 *   <li>{@code vmd.vehcile-part-binding.changed}：车辆-零件绑定变更事件，
 *       由 {@code VehiclePartBindingKafkaProducer} 直发</li>
 *   <li>{@code vmd.vehicle-produce}：车辆生产事件，经 vmd_outbox → OutboxRelay 发布/补发</li>
 *   <li>{@code vmd.vehicle-software-inventory.changed}：车辆软件清单变更事件，
 *       经 vmd_outbox → OutboxRelay 发布</li>
 * </ul>
 * <p>
 * 不声明 upstream.*（mdm.*、ota.* 等消费 Topic）或 DLQ Topic；消费 Topic
 * ota.vehicle-software-inventory.observed 由 IOV-OTA 管理，VMD 不创建。
 * Topic 名称统一来自 {@link VmdKafkaTopicProperties}，分区数、副本数与 cleanup.policy
 * 通过 {@link VmdKafkaTopicProvisioningProperties} 环境参数注入。
 *
 * @author hwyz_leo
 */
@Component
@RequiredArgsConstructor
public class VmdKafkaTopicDefinitionProvider implements KafkaTopicDefinitionProvider {

    private final VmdKafkaTopicProvisioningProperties provisioningProperties;
    private final VmdKafkaTopicProperties topicProperties;

    @Override
    public Collection<KafkaTopicDefinition> topicDefinitions() {
        List<VmdKafkaLogicalTopic> produced = List.of(
                VmdKafkaLogicalTopic.PART_BINDING_CHANGED,
                VmdKafkaLogicalTopic.VEHICLE_PRODUCE,
                VmdKafkaLogicalTopic.SOFTWARE_INVENTORY_CHANGED);

        List<KafkaTopicDefinition> definitions = new ArrayList<>(produced.size());
        for (VmdKafkaLogicalTopic logical : produced) {
            definitions.add(new KafkaTopicDefinition(
                    topicProperties.topic(logical),
                    provisioningProperties.getPartitions(),
                    provisioningProperties.getReplicationFactor(),
                    provisioningProperties.configs()));
        }
        return definitions;
    }
}
