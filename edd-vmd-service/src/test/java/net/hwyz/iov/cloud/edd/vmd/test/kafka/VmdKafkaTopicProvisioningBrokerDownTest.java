package net.hwyz.iov.cloud.edd.vmd.test.kafka;

import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicCatalog;
import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicProvisioningStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * VMD Kafka Topic Provisioning 装配测试（VMD-DSN-CR-051）
 * <p>
 * broker 指向不可达地址：服务保持正常就绪，Provisioning 状态为 NOT_READY，
 * 验证 VMD 声明的三个标准生产 Topic 的 Catalog 合并与后台重试机制正常装配。
 *
 * @author hwyz_leo
 */
@SpringBootTest(
        classes = VmdKafkaProvisioningTestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "iov.kafka.topic-provisioning.enabled=true",
                "iov.kafka.topic-provisioning.initial-delay=0s",
                "iov.kafka.topic-provisioning.retry.initial-interval=1s",
                "iov.kafka.topic-provisioning.retry.max-interval=2s",
                "iov.kafka.topic-provisioning.retry.multiplier=2.0",
                "spring.kafka.bootstrap-servers=127.0.0.1:1",
                "spring.kafka.admin.properties.request.timeout.ms=2000",
                "spring.kafka.admin.properties.default.api.timeout.ms=2000",
                "spring.cloud.bootstrap.enabled=false",
                "spring.cloud.nacos.config.enabled=false",
                "spring.cloud.nacos.discovery.enabled=false",
                "vmd.kafka.topic-provisioning.partitions=1",
                "vmd.kafka.topic-provisioning.replication-factor=1"
        })
@DisplayName("VMD Kafka Topic Provisioning 装配测试")
class VmdKafkaTopicProvisioningBrokerDownTest {

    @Autowired
    private KafkaTopicCatalog kafkaTopicCatalog;

    @Autowired
    private KafkaTopicProvisioningStatus provisioningStatus;

    @Test
    @DisplayName("broker 不可用时服务保持就绪，Catalog 声明三个标准生产 Topic，状态 NOT_READY 并记录失败")
    void brokerUnavailable_serviceReady_catalogDeclared_notReady() throws Exception {
        // 服务正常启动（上下文已加载）
        assertNotNull(provisioningStatus);
        assertNotNull(kafkaTopicCatalog);

        // Catalog 合并了 VMD 声明的三个标准生产 Topic（VMD-DSN-CR-051）
        assertTrue(kafkaTopicCatalog.contains("vmd.vehcile-part-binding.changed"),
                "Catalog 应包含 vmd.vehcile-part-binding.changed");
        assertTrue(kafkaTopicCatalog.contains("vmd.vehicle-produce"),
                "Catalog 应包含 vmd.vehicle-produce");
        assertTrue(kafkaTopicCatalog.contains("vmd.vehicle-software-inventory.changed"),
                "Catalog 应包含 vmd.vehicle-software-inventory.changed");
        assertEquals(3, kafkaTopicCatalog.definitions().size());

        // 消费 Topic ota.vehicle-software-inventory.observed 不由 VMD 声明
        assertTrue(!kafkaTopicCatalog.contains("ota.vehicle-software-inventory.observed"),
                "Catalog 不应包含 ota.* 消费 Topic（VMD 不创建）");

        // broker 不可达，Provisioning 状态为 NOT_READY
        assertEquals(KafkaTopicProvisioningStatus.State.NOT_READY, provisioningStatus.state());

        // 等待后台重试发生，状态保持 NOT_READY 并记录失败原因（Admin 超时 2s，此处预留充足余量）
        long deadline = System.currentTimeMillis() + 10000;
        while (System.currentTimeMillis() < deadline) {
            if (provisioningStatus.lastFailure().isPresent()) {
                break;
            }
            Thread.sleep(500);
        }
        assertTrue(provisioningStatus.lastFailure().isPresent(), "后台重试后应记录 broker 失败原因");
        assertEquals(KafkaTopicProvisioningStatus.State.NOT_READY, provisioningStatus.state());
        assertEquals(3, provisioningStatus.missingTopics().size(),
                "缺失 Topic 应为全部声明 Topic");
    }
}
