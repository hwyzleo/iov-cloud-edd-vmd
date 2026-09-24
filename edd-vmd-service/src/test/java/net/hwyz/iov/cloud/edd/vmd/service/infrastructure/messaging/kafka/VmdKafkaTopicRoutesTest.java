package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.VmdKafkaTopicProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * VMD Kafka Topic 路由注册表单元测试（VMD-DSN-CR-051）
 * <p>
 * 验证事件类型→标准 Topic 映射与未知逻辑事件类型 fail-fast，
 * 禁止字符串拼接推导 Topic。
 *
 * @author hwyz_leo
 */
@DisplayName("VmdKafkaTopicRoutes 测试")
class VmdKafkaTopicRoutesTest {

    private VmdKafkaTopicRoutes routes;

    @BeforeEach
    void setUp() {
        VmdKafkaTopicProperties properties = new VmdKafkaTopicProperties();
        routes = new VmdKafkaTopicRoutes(properties);
    }

    @Test
    @DisplayName("三类领域事件解析到目录标准 Topic（Key/payload 语义不变）")
    void resolveRegisteredEventTypes() {
        assertEquals("vmd.vehicle-produce",
                routes.resolve("VehicleProduceEvent"));
        assertEquals("vmd.vehcile-part-binding.changed",
                routes.resolve("VehiclePartBindingChangedEvent"));
        assertEquals("vmd.vehicle-software-inventory.changed",
                routes.resolve("VehicleSoftwareInventoryChangedEvent"));
    }

    @Test
    @DisplayName("未知逻辑事件类型 fail-fast，不拼接推导 Topic")
    void unknownEventTypeFailsFast() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> routes.resolve("UnknownCustomEvent"),
                "未知逻辑事件类型应 fail-fast");
        assertTrue(ex.getMessage().contains("UnknownCustomEvent"));
        assertThrows(IllegalArgumentException.class, () -> routes.logicalTopic("UnknownCustomEvent"));
        assertFalse(routes.isRegistered("UnknownCustomEvent"));
        assertEquals(Optional.empty(), routes.resolveIfRegistered("UnknownCustomEvent"));
    }

    @Test
    @DisplayName("logicalTopic 返回事件类型对应逻辑名")
    void logicalTopicMapping() {
        assertEquals(VmdKafkaLogicalTopic.VEHICLE_PRODUCE, routes.logicalTopic("VehicleProduceEvent"));
        assertEquals(VmdKafkaLogicalTopic.PART_BINDING_CHANGED, routes.logicalTopic("VehiclePartBindingChangedEvent"));
        assertEquals(VmdKafkaLogicalTopic.SOFTWARE_INVENTORY_CHANGED, routes.logicalTopic("VehicleSoftwareInventoryChangedEvent"));
    }

    @Test
    @DisplayName("逻辑名按配置返回实际 Topic")
    void topicNameByLogical() {
        assertEquals("ota.vehicle-software-inventory.observed",
                routes.topicName(VmdKafkaLogicalTopic.INVENTORY_OBSERVED));
        assertEquals("vmd.vehcile-part-binding.changed",
                routes.topicName(VmdKafkaLogicalTopic.PART_BINDING_CHANGED));
    }
}
