package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.VmdKafkaTopicProperties;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.VmdKafkaTopicProvisioningProperties;
import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * VMD Kafka Topic 定义提供者单元测试（VMD-DSN-CR-051）
 * <p>
 * 验证 VMD 作为生产者输出的三个标准 Topic 声明：
 * vmd.vehcile-part-binding.changed（绑定变更事件直发）、
 * vmd.vehicle-produce（车辆生产事件 Outbox）、
 * vmd.vehicle-software-inventory.changed（软件清单变更事件 Outbox）；
 * 不声明 upstream.*（mdm.* / ota.* 消费 Topic）与 DLQ Topic。
 *
 * @author hwyz_leo
 */
@DisplayName("VmdKafkaTopicDefinitionProvider 测试")
class VmdKafkaTopicDefinitionProviderTest {

    private VmdKafkaTopicDefinitionProvider provider;

    @BeforeEach
    void setUp() {
        VmdKafkaTopicProvisioningProperties provisioningProperties = new VmdKafkaTopicProvisioningProperties();
        provisioningProperties.setPartitions(3);
        provisioningProperties.setReplicationFactor((short) 3);
        provisioningProperties.setCleanupPolicy("delete");

        VmdKafkaTopicProperties topicProperties = new VmdKafkaTopicProperties();
        topicProperties.setPartBindingChanged("vmd.vehcile-part-binding.changed");
        topicProperties.setVehicleProduce("vmd.vehicle-produce");
        topicProperties.setSoftwareInventoryChanged("vmd.vehicle-software-inventory.changed");

        provider = new VmdKafkaTopicDefinitionProvider(provisioningProperties, topicProperties);
    }

    private Set<String> declaredTopics() {
        Collection<KafkaTopicDefinition> definitions = provider.topicDefinitions();
        return definitions.stream().map(KafkaTopicDefinition::name).collect(Collectors.toSet());
    }

    @Test
    @DisplayName("声明 VMD 作为生产者输出的全部标准 Topic")
    void declaresAllVmdProducedTopics() {
        Set<String> topics = declaredTopics();
        assertTrue(topics.contains("vmd.vehcile-part-binding.changed"),
                "缺少 Topic: vmd.vehcile-part-binding.changed");
        assertTrue(topics.contains("vmd.vehicle-produce"),
                "缺少 Topic: vmd.vehicle-produce");
        assertTrue(topics.contains("vmd.vehicle-software-inventory.changed"),
                "缺少 Topic: vmd.vehicle-software-inventory.changed");
    }

    @Test
    @DisplayName("不声明 upstream.* / DLQ / 清单外 Topic（含消费 Topic ota.*）")
    void doesNotDeclareOutOfScopeTopics() {
        Set<String> topics = declaredTopics();
        assertTrue(topics.stream().noneMatch(t -> t.startsWith("mdm.")),
                "不应声明 mdm.*（上游 MDM 消费 Topic）");
        assertTrue(topics.stream().noneMatch(t -> t.startsWith("ota.")),
                "不应声明 ota.*（上游 OTA 消费 Topic，VMD 不创建）");
        assertTrue(topics.stream().noneMatch(t -> t.contains(".dlq") || t.contains("DLQ")),
                "不应声明 DLQ Topic");
    }

    @Test
    @DisplayName("总计 3 个 topic，无重复")
    void declaresExpectedTotalWithNoDuplicates() {
        Collection<KafkaTopicDefinition> definitions = provider.topicDefinitions();
        Set<String> names = declaredTopics();
        assertEquals(3, names.size(), "VMD 应声明 3 个生产 topic");
        assertEquals(definitions.size(), names.size(), "声明不应包含重复 topic");
    }

    @Test
    @DisplayName("全部 Definition 携带环境注入的分区、副本数与 cleanup.policy")
    void definitionsCarryConfiguredPartitionsReplicationAndConfigs() {
        for (KafkaTopicDefinition def : provider.topicDefinitions()) {
            assertEquals(3, def.partitions());
            assertEquals((short) 3, def.replicationFactor());
            assertEquals("delete", def.configs().get("cleanup.policy"));
            assertTrue(def.configs() instanceof Map);
        }
    }

    @Test
    @DisplayName("Topic 名称与类型化配置保持一致（环境变量覆盖时声明实际 topic）")
    void topicNamesAlignWithTopicProperties() {
        VmdKafkaTopicProperties overridden = new VmdKafkaTopicProperties();
        overridden.setVehicleProduce("vmd-vehicle-produce-override");
        provider = new VmdKafkaTopicDefinitionProvider(new VmdKafkaTopicProvisioningProperties(), overridden);
        assertTrue(declaredTopics().contains("vmd-vehicle-produce-override"));
    }
}
