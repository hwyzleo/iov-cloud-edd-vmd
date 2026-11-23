package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.VmdActivationFactConsumerProperties;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.VmdKafkaTopicProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 首次激活事实消费 Topic 登记单元测试（VMD-DSN-CR-077 / D-VMD-077-01，R-18 #3）
 * <p>
 * 验证：CONSUMER_ONLY 登记、容器 id 常量、Topic 目录与类型化配置逐字符一致、
 * 重复值 fail-fast、不混入 {@link MdmProjectionType}（保持 11 个 MDM 投影）、
 * 不进入 Outbox 生产路由（纯终端消费者，D-VMD-077-10）。
 *
 * @author hwyz_leo
 */
@DisplayName("VmdConsumeType / Topic 目录登记测试")
class VmdActivationFactTopicRegistrationTest {

    private static final String DIRECTORY_TOPIC = "tsp.user-vehicle-activation-fact";

    @Test
    @DisplayName("消费类型登记为 CONSUMER_ONLY，容器 id 常量化且与目录值一致")
    void consumerType_registeredAsConsumerOnly() {
        VmdConsumeType consumeType = VmdConsumeType.USER_VEHICLE_ACTIVATION_FACT;

        assertEquals(VmdKafkaTopicRole.CONSUMER_ONLY, consumeType.role(), "上游 Topic 必须只消费不创建");
        assertEquals(DIRECTORY_TOPIC, consumeType.directoryTopic());
        assertEquals("user-vehicle-activation-fact", consumeType.configKey());
        assertEquals("user-vehicle-activation-fact-consumer", consumeType.consumerId());
        assertEquals(VmdConsumeType.ConsumerIds.USER_VEHICLE_ACTIVATION_FACT, consumeType.consumerId(),
                "容器 id 必须来自常量表，供 @KafkaListener(id=...) 编译期引用");
        assertEquals(VmdKafkaLogicalTopic.USER_VEHICLE_ACTIVATION_FACT, consumeType.logicalTopic());
    }

    @Test
    @DisplayName("逻辑 Topic 目录登记为 CONSUMER_ONLY，且不进入生产声明集")
    void logicalTopic_consumerOnly() {
        assertEquals(VmdKafkaTopicRole.CONSUMER_ONLY,
                VmdKafkaLogicalTopic.USER_VEHICLE_ACTIVATION_FACT.role());
        long producerOwned = Arrays.stream(VmdKafkaLogicalTopic.values())
                .filter(logical -> logical.role() == VmdKafkaTopicRole.PRODUCER_OWNED)
                .count();
        assertEquals(4, producerOwned, "生产 Topic 应恰为 4 个，TSP 消费 Topic 不得进入生产声明集");
    }

    @Test
    @DisplayName("类型化配置默认值与目录值逐字符一致，且可经配置覆盖")
    void properties_defaultsMatchDirectory() {
        VmdKafkaTopicProperties properties = new VmdKafkaTopicProperties();

        assertEquals(DIRECTORY_TOPIC, properties.getUserVehicleActivationFact());
        assertEquals(DIRECTORY_TOPIC, properties.topic(VmdKafkaLogicalTopic.USER_VEHICLE_ACTIVATION_FACT));
        assertEquals(DIRECTORY_TOPIC, properties.topic(VmdConsumeType.USER_VEHICLE_ACTIVATION_FACT.logicalTopic()));

        properties.setUserVehicleActivationFact("tsp.user-vehicle-activation-fact.e2e");
        assertEquals("tsp.user-vehicle-activation-fact.e2e",
                properties.topic(VmdKafkaLogicalTopic.USER_VEHICLE_ACTIVATION_FACT));
    }

    @Test
    @DisplayName("重复 Topic 值 fail-fast（与其它逻辑键撞名）")
    void duplicateTopicFailsFast() {
        VmdKafkaTopicProperties properties = new VmdKafkaTopicProperties();
        properties.setUserVehicleActivationFact(properties.getInventoryObserved());

        IllegalStateException ex = assertThrows(IllegalStateException.class, properties::validate);
        assertTrue(ex.getMessage().contains("重复"));
    }

    @Test
    @DisplayName("空 Topic 值 fail-fast")
    void blankTopicFailsFast() {
        VmdKafkaTopicProperties properties = new VmdKafkaTopicProperties();
        properties.setUserVehicleActivationFact(" ");

        IllegalStateException ex = assertThrows(IllegalStateException.class, properties::validate);
        assertTrue(ex.getMessage().contains("user-vehicle-activation-fact"));
    }

    @Test
    @DisplayName("MDM 投影类型仍恰为 11 个（不混入 TSP 消费 Topic）")
    void mdmProjectionTypeUnchanged() {
        assertEquals(11, MdmProjectionType.values().length);
        assertFalse(Arrays.stream(MdmProjectionType.values())
                        .anyMatch(p -> p.directoryTopic().equals(DIRECTORY_TOPIC)),
                "TSP 激活事实 Topic 不得混入 MDM 投影注册表（避免语义污染与 MDM 预检误报）");
    }

    @Test
    @DisplayName("纯终端消费者：事件类型不进入 Outbox 生产路由（不写 outbox、不发二次事件）")
    void notInOutboxRoutes() {
        VmdKafkaTopicRoutes routes = new VmdKafkaTopicRoutes(new VmdKafkaTopicProperties());

        assertFalse(routes.isRegistered("UserVehicleActivationFactEvent"),
                "激活事实为终端消费，不得注册为 Outbox 发布事件");
        assertTrue(routes.resolveIfRegistered("UserVehicleActivationFactEvent").isEmpty());
    }

    @Test
    @DisplayName("消费配置默认暗部署关闭 + auto-offset-reset=earliest + 退避 3 次")
    void consumerPropertiesDefaults() {
        VmdActivationFactConsumerProperties properties = new VmdActivationFactConsumerProperties();

        assertFalse(properties.isEnabled(), "默认暗部署关闭（生产默认仍为消费关闭）");
        assertEquals("earliest", properties.getAutoOffsetReset(),
                "终审 R-8：默认 earliest，开启前回放历史事实，补齐早期已激活车辆的首次激活节点");
        assertEquals("edd-vmd-vehicle-activation-fact", properties.getGroupId());
        assertEquals(3, properties.getRetry().getMaxAttempts());
        assertEquals(100L, properties.getRetry().getInitialIntervalMs());
        assertEquals(1000L, properties.getRetry().getMaxIntervalMs());
        assertFalse(properties.isStartupGateEnabled(), "默认容错启动：Topic 缺失不阻塞启动");
    }

}
