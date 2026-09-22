package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.VmdKafkaTopicProvisioningProperties;
import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * VMD Kafka Topic 定义提供者单元测试
 * <p>
 * 验证 VMD 作为生产者输出的 Topic 声明：
 * vmd-vehicle-binding-changed（绑定变更事件直发）与
 * vmd-vehicle-software-inventory-changed（软件清单变更事件经 Outbox 发布）；
 * 不声明 upstream.*（mdm.* / ota.* 消费 Topic）与 DLQ Topic。
 *
 * @author hwyz_leo
 */
@DisplayName("VmdKafkaTopicDefinitionProvider 测试")
class VmdKafkaTopicDefinitionProviderTest {

    private VmdKafkaTopicDefinitionProvider provider;

    @BeforeEach
    void setUp() {
        VmdKafkaTopicProvisioningProperties properties = new VmdKafkaTopicProvisioningProperties();
        properties.setPartitions(3);
        properties.setReplicationFactor((short) 3);
        provider = new VmdKafkaTopicDefinitionProvider(properties);
        ReflectionTestUtils.setField(provider, "bindingChangedTopic", "vmd-vehicle-binding-changed");
        ReflectionTestUtils.setField(provider, "softwareInventoryChangedTopic", "vmd-vehicle-software-inventory-changed");
    }

    private Set<String> declaredTopics() {
        Collection<KafkaTopicDefinition> definitions = provider.topicDefinitions();
        return definitions.stream().map(KafkaTopicDefinition::name).collect(Collectors.toSet());
    }

    @Test
    @DisplayName("声明 VMD 作为生产者输出的全部 Topic")
    void declaresAllVmdProducedTopics() {
        Set<String> topics = declaredTopics();
        assertTrue(topics.contains("vmd-vehicle-binding-changed"),
                "缺少 Topic: vmd-vehicle-binding-changed");
        assertTrue(topics.contains("vmd-vehicle-software-inventory-changed"),
                "缺少 Topic: vmd-vehicle-software-inventory-changed");
    }

    @Test
    @DisplayName("不声明 upstream.* / DLQ / 清单外 Topic")
    void doesNotDeclareOutOfScopeTopics() {
        Set<String> topics = declaredTopics();
        assertTrue(topics.stream().noneMatch(t -> t.startsWith("mdm.")),
                "不应声明 mdm.*（上游 MDM 消费 Topic）");
        assertTrue(topics.stream().noneMatch(t -> t.startsWith("ota.")),
                "不应声明 ota.*（上游 OTA 消费 Topic）");
        assertTrue(topics.stream().noneMatch(t -> t.contains(".dlq") || t.contains("DLQ")),
                "不应声明 DLQ Topic");
        assertTrue(topics.stream().allMatch(t -> t.startsWith("vmd-")),
                "只应声明 VMD 自身的 vmd-* Topic");
    }

    @Test
    @DisplayName("总计 2 个 topic，无重复")
    void declaresExpectedTotalWithNoDuplicates() {
        Collection<KafkaTopicDefinition> definitions = provider.topicDefinitions();
        Set<String> names = declaredTopics();
        assertEquals(2, names.size(), "VMD 应声明 2 个 topic");
        assertEquals(definitions.size(), names.size(), "声明不应包含重复 topic");
    }

    @Test
    @DisplayName("全部 Definition 携带环境注入的分区与副本数")
    void definitionsCarryConfiguredPartitionsAndReplication() {
        for (KafkaTopicDefinition def : provider.topicDefinitions()) {
            assertEquals(3, def.partitions());
            assertEquals((short) 3, def.replicationFactor());
        }
    }

    @Test
    @DisplayName("Topic 名称与生产者/Outbox 配置保持一致（环境变量覆盖时声明实际 topic）")
    void topicNamesAlignWithProducerConfig() {
        ReflectionTestUtils.setField(provider, "bindingChangedTopic", "vmd-binding-override");
        assertTrue(declaredTopics().contains("vmd-binding-override"));
        assertFalse(declaredTopics().contains("vmd-vehicle-binding-changed"));
    }
}
