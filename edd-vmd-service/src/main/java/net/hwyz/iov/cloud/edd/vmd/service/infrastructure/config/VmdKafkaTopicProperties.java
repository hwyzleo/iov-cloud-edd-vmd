package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.VmdKafkaLogicalTopic;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * VMD Kafka Topic 目录类型化配置（VMD-DSN-CR-051）
 * <p>
 * 四个 EDD-VMD 相关 Topic 的 SSOT：Listener 占位符、Producer、Outbox 记录与
 * Relay 路由统一引用本配置，禁止在注解、业务类与测试中复制 Topic 字符串。
 * <p>
 * 默认值为开发环境默认值，生产环境必须显式配置（Nacos 覆盖）。
 *
 * @author hwyz_leo
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "vmd.kafka.topics")
public class VmdKafkaTopicProperties {

    /**
     * OTA 车辆软件观测事件 Topic（Consumer，IOV-OTA 管理，VMD 不创建）
     */
    @NotBlank
    private String inventoryObserved = "ota.vehicle-software-inventory.observed";

    /**
     * 车辆-零件绑定变更事件 Topic（Producer，VMD 幂等创建）
     * <p>目录治理值，保留 vehcile 拼写（RD-051-6）
     */
    @NotBlank
    private String partBindingChanged = "vmd.vehcile-part-binding.changed";

    /**
     * 车辆生产事件 Topic（Producer，VMD 幂等创建，经 Outbox→Relay 发布/补发）
     */
    @NotBlank
    private String vehicleProduce = "vmd.vehicle-produce";

    /**
     * 车辆软件清单变更事件 Topic（Producer，VMD 幂等创建，经 Outbox→Relay 发布）
     */
    @NotBlank
    private String softwareInventoryChanged = "vmd.vehicle-software-inventory.changed";

    @PostConstruct
    public void init() {
        validate();
    }

    /**
     * 启动期校验：必填（@NotBlank）+ 重复 Topic 值 fail-fast。
     * <p>同一 Topic 名称被多个逻辑键占用属于配置错误，直接拒绝启动。
     */
    public void validate() {
        Map<String, String> values = new HashMap<>();
        values.put("inventory-observed", inventoryObserved);
        values.put("part-binding-changed", partBindingChanged);
        values.put("vehicle-produce", vehicleProduce);
        values.put("software-inventory-changed", softwareInventoryChanged);

        Set<String> seen = new HashSet<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String topic = entry.getValue();
            if (topic == null || topic.isBlank()) {
                throw new IllegalStateException("VMD Kafka Topic 配置缺失: vmd.kafka.topics." + entry.getKey());
            }
            if (!seen.add(topic)) {
                throw new IllegalStateException("VMD Kafka Topic 配置存在重复值: " + topic
                        + " 被 vmd.kafka.topics." + entry.getKey() + " 与其它逻辑键重复引用");
            }
        }
    }

    /**
     * 按逻辑名读取实际 Topic 名称。
     *
     * @param logical 逻辑名
     * @return 实际 Topic 名称
     */
    public String topic(VmdKafkaLogicalTopic logical) {
        return switch (logical) {
            case INVENTORY_OBSERVED -> inventoryObserved;
            case PART_BINDING_CHANGED -> partBindingChanged;
            case VEHICLE_PRODUCE -> vehicleProduce;
            case SOFTWARE_INVENTORY_CHANGED -> softwareInventoryChanged;
        };
    }
}
