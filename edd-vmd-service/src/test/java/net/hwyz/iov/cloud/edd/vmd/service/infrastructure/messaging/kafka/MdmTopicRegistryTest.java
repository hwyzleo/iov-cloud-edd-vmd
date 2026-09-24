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
 * MDM 投影 → Topic 显式注册表单元测试（VMD-DSN-CR-052，RD-052-3）
 * <p>
 * 验证 11 个投影条目映射正确、未知投影类型 fail-fast、禁止字符串拼接推导 Topic。
 *
 * @author hwyz_leo
 */
@DisplayName("MdmTopicRegistry 测试")
class MdmTopicRegistryTest {

    private MdmTopicRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new MdmTopicRegistry(new VmdKafkaTopicProperties());
    }

    @Test
    @DisplayName("11 个投影类型均解析到目录标准 Topic")
    void allProjectionsResolveDirectoryTopic() {
        assertEquals(MdmProjectionType.values().length, 11);
        for (MdmProjectionType projection : MdmProjectionType.values()) {
            assertEquals(projection.directoryTopic(), registry.topicName(projection),
                    "投影 " + projection.configKey() + " 应解析到目录标准值");
        }
    }

    @Test
    @DisplayName("consumerId 与 ConsumerIds 常量表一致（@KafkaListener 单一事实来源）")
    void consumerIdMatchesConstants() {
        assertEquals(MdmProjectionType.ConsumerIds.BRAND, registry.consumerId(MdmProjectionType.BRAND));
        assertEquals(MdmProjectionType.ConsumerIds.CAR_LINE, registry.consumerId(MdmProjectionType.CAR_LINE));
        assertEquals(MdmProjectionType.ConsumerIds.CONFIGURATION, registry.consumerId(MdmProjectionType.CONFIGURATION));
        assertEquals(MdmProjectionType.ConsumerIds.MODEL, registry.consumerId(MdmProjectionType.MODEL));
        assertEquals(MdmProjectionType.ConsumerIds.OPTION_CODE, registry.consumerId(MdmProjectionType.OPTION_CODE));
        assertEquals(MdmProjectionType.ConsumerIds.OPTION_FAMILY, registry.consumerId(MdmProjectionType.OPTION_FAMILY));
        assertEquals(MdmProjectionType.ConsumerIds.PART, registry.consumerId(MdmProjectionType.PART));
        assertEquals(MdmProjectionType.ConsumerIds.PLANT, registry.consumerId(MdmProjectionType.PLANT));
        assertEquals(MdmProjectionType.ConsumerIds.PLATFORM, registry.consumerId(MdmProjectionType.PLATFORM));
        assertEquals(MdmProjectionType.ConsumerIds.VARIANT, registry.consumerId(MdmProjectionType.VARIANT));
        assertEquals(MdmProjectionType.ConsumerIds.VEHICLE_NODE, registry.consumerId(MdmProjectionType.VEHICLE_NODE));
    }

    @Test
    @DisplayName("isRegistered / findByConfigKey 覆盖全部配置键")
    void configKeyLookup() {
        for (MdmProjectionType projection : MdmProjectionType.values()) {
            assertTrue(registry.isRegistered(projection.configKey()));
            assertEquals(Optional.of(projection), registry.findByConfigKey(projection.configKey()));
            assertEquals(projection, registry.fromConfigKey(projection.configKey()));
        }
    }

    @Test
    @DisplayName("未知配置键 fail-fast，不拼接推导")
    void unknownConfigKeyFailsFast() {
        assertFalse(registry.isRegistered("unknown-projection"));
        assertEquals(Optional.empty(), registry.findByConfigKey("unknown-projection"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> registry.fromConfigKey("unknown-projection"),
                "未知投影配置键应 fail-fast");
        assertTrue(ex.getMessage().contains("unknown-projection"));
    }
}
