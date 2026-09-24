package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config;

import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.MdmProjectionType;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdKafkaLogicalTopic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * VMD Kafka Topic 目录类型化配置单元测试（VMD-DSN-CR-051 / VMD-DSN-CR-052）
 * <p>
 * 验证四个自有 Topic + 11 个 MDM 消费 Topic 默认值与 Kafka Topic 目录标准值逐字符一致、
 * 缺少属性 / 重复值 / 与自有 Topic 撞名 fail-fast，以及投影解析与环境覆盖。
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

    @Test
    @DisplayName("11 个 MDM 消费 Topic 默认值与目录标准值逐字符一致（CR-052）")
    void mdmDefaultsMatchTopicDirectory() {
        VmdKafkaTopicProperties properties = new VmdKafkaTopicProperties();
        VmdKafkaTopicProperties.MdmConsumerTopics mdm = properties.getMdm();
        assertEquals("mdm.brand", mdm.getBrand());
        assertEquals("mdm.car-line", mdm.getCarLine());
        assertEquals("mdm.configuration", mdm.getConfiguration());
        assertEquals("mdm.model", mdm.getModel());
        assertEquals("mdm.option-code", mdm.getOptionCode());
        assertEquals("mdm.option-family", mdm.getOptionFamily());
        assertEquals("mdm.part", mdm.getPart());
        assertEquals("mdm.plant", mdm.getPlant());
        assertEquals("mdm.platform", mdm.getPlatform());
        assertEquals("mdm.variant", mdm.getVariant());
        assertEquals("mdm.vehicle-node", mdm.getVehicleNode());
    }

    @Test
    @DisplayName("topic(MdmProjectionType) 与 directoryTopic 逐字符一致且无遗漏（11/11）")
    void mdmProjectionResolvesDirectoryTopic() {
        VmdKafkaTopicProperties properties = new VmdKafkaTopicProperties();
        for (MdmProjectionType projection : MdmProjectionType.values()) {
            assertEquals(projection.directoryTopic(), properties.topic(projection),
                    "投影 " + projection.configKey() + " 应解析到目录标准值");
        }
        assertEquals(MdmProjectionType.values().length, 11, "MDM 投影类型必须恰为 11 个");
    }

    @Test
    @DisplayName("MDM 消费 Topic 空值 fail-fast（CR-052 §3 约束 1）")
    void mdmBlankFailsFast() {
        VmdKafkaTopicProperties properties = new VmdKafkaTopicProperties();
        properties.getMdm().setPart(" ");
        IllegalStateException ex = assertThrows(IllegalStateException.class, properties::validate,
                "MDM 消费 Topic 空值应 fail-fast");
        assertEquals(true, ex.getMessage().contains("mdm.part"));
    }

    @Test
    @DisplayName("MDM 消费 Topic 与自有生产 Topic 撞名 fail-fast（CR-052 §3 约束 2）")
    void mdmCollidesWithOwnedTopicFailsFast() {
        VmdKafkaTopicProperties properties = new VmdKafkaTopicProperties();
        properties.getMdm().setBrand(properties.getVehicleProduce());
        IllegalStateException ex = assertThrows(IllegalStateException.class, properties::validate,
                "MDM 消费 Topic 与自有生产 Topic 撞名应 fail-fast");
        assertEquals(true, ex.getMessage().contains("重复"));
    }

    @Test
    @DisplayName("MDM 消费 Topic 内部重复 fail-fast")
    void mdmInternalDuplicateFailsFast() {
        VmdKafkaTopicProperties properties = new VmdKafkaTopicProperties();
        properties.getMdm().setOptionCode(properties.getMdm().getOptionFamily());
        IllegalStateException ex = assertThrows(IllegalStateException.class, properties::validate,
                "MDM 消费 Topic 内部重复应 fail-fast");
        assertEquals(true, ex.getMessage().contains("重复"));
    }

    @Test
    @DisplayName("环境覆盖后按投影返回覆盖值（CR-052）")
    void mdmProjectionReturnsOverriddenValue() {
        VmdKafkaTopicProperties properties = new VmdKafkaTopicProperties();
        properties.getMdm().setBrand("mdm.brand.prod");
        assertEquals("mdm.brand.prod", properties.topic(MdmProjectionType.BRAND));
    }
}