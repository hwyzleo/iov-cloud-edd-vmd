package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdKafkaLogicalTopic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * VMD Kafka Topic 目录类型化配置单元测试（VMD-DSN-CR-051）
 * <p>
 * 验证四个逻辑键默认值与 Kafka Topic 目录标准值逐字符一致、
 * 缺少属性与重复 Topic 值 fail-fast。
 *
 * @author hwyz_leo
 */
@DisplayName("VmdKafkaTopicProperties 测试")
class VmdKafkaTopicPropertiesTest {

    @Test
    @DisplayName("默认值与 Kafka Topic 目录标准值逐字符一致")
    void defaultsMatchTopicDirectory() {
        VmdKafkaTopicProperties properties = new VmdKafkaTopicProperties();
        assertEquals("ota.vehicle-software-inventory.observed", properties.getInventoryObserved());
        assertEquals("vmd.vehcile-part-binding.changed", properties.getPartBindingChanged());
        assertEquals("vmd.vehicle-produce", properties.getVehicleProduce());
        assertEquals("vmd.vehicle-software-inventory.changed", properties.getSoftwareInventoryChanged());
    }

    @Test
    @DisplayName("topic(logical) 按逻辑名返回实际 Topic")
    void topicReturnsByLogicalName() {
        VmdKafkaTopicProperties properties = new VmdKafkaTopicProperties();
        assertEquals("ota.vehicle-software-inventory.observed",
                properties.topic(VmdKafkaLogicalTopic.INVENTORY_OBSERVED));
        assertEquals("vmd.vehcile-part-binding.changed",
                properties.topic(VmdKafkaLogicalTopic.PART_BINDING_CHANGED));
        assertEquals("vmd.vehicle-produce",
                properties.topic(VmdKafkaLogicalTopic.VEHICLE_PRODUCE));
        assertEquals("vmd.vehicle-software-inventory.changed",
                properties.topic(VmdKafkaLogicalTopic.SOFTWARE_INVENTORY_CHANGED));
    }

    @Test
    @DisplayName("环境覆盖后按逻辑名返回覆盖值")
    void topicReturnsOverriddenValue() {
        VmdKafkaTopicProperties properties = new VmdKafkaTopicProperties();
        properties.setVehicleProduce("vmd.vehicle-produce.prod");
        assertEquals("vmd.vehicle-produce.prod", properties.topic(VmdKafkaLogicalTopic.VEHICLE_PRODUCE));
    }

    @Test
    @DisplayName("缺少必填属性 fail-fast")
    void blankValueFailsFast() {
        VmdKafkaTopicProperties properties = new VmdKafkaTopicProperties();
        properties.setVehicleProduce("");
        IllegalStateException ex = assertThrows(IllegalStateException.class, properties::validate,
                "缺少必填属性应 fail-fast");
        assertEquals(true, ex.getMessage().contains("vmd.kafka.topics.vehicle-produce"));
    }

    @Test
    @DisplayName("重复 Topic 值 fail-fast")
    void duplicateValueFailsFast() {
        VmdKafkaTopicProperties properties = new VmdKafkaTopicProperties();
        properties.setSoftwareInventoryChanged(properties.getVehicleProduce());
        IllegalStateException ex = assertThrows(IllegalStateException.class, properties::validate,
                "重复 Topic 值应 fail-fast");
        assertEquals(true, ex.getMessage().contains("重复"));
    }
}
