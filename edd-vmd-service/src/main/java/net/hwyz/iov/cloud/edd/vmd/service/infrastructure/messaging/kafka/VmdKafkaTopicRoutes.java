package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.VmdKafkaTopicProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * VMD Kafka Topic 路由注册表（VMD-DSN-CR-051）
 * <p>
 * Outbox 记录写入与 Relay 发布路由统一引用本注册表：按事件类型解析标准 Topic，
 * 未知逻辑事件类型 fail-fast，禁止通过默认字符串拼接推导 Topic。
 * <p>
 * 注册表为 SSOT 入口，实际 Topic 名称由 {@link VmdKafkaTopicProperties} 提供，
 * 事件类型→逻辑名映射固定如下：
 * <ul>
 *   <li>{@code VehicleProduceEvent} → {@code vehicle-produce}</li>
 *   <li>{@code VehiclePartBindingChangedEvent} → {@code part-binding-changed}</li>
 *   <li>{@code VehicleSoftwareInventoryChangedEvent} → {@code software-inventory-changed}</li>
 * </ul>
 *
 * @author hwyz_leo
 */
@Component
@RequiredArgsConstructor
public class VmdKafkaTopicRoutes {

    private final VmdKafkaTopicProperties topicProperties;

    /**
     * 事件类型 → 逻辑 Topic 映射（注册表）
     */
    private static final Map<String, VmdKafkaLogicalTopic> EVENT_TYPE_ROUTES = Map.of(
            "VehicleProduceEvent", VmdKafkaLogicalTopic.VEHICLE_PRODUCE,
            "VehiclePartBindingChangedEvent", VmdKafkaLogicalTopic.PART_BINDING_CHANGED,
            "VehicleSoftwareInventoryChangedEvent", VmdKafkaLogicalTopic.SOFTWARE_INVENTORY_CHANGED
    );

    /**
     * 按逻辑名读取实际 Topic 名称。
     *
     * @param logical 逻辑名
     * @return 实际 Topic 名称
     */
    public String topicName(VmdKafkaLogicalTopic logical) {
        return topicProperties.topic(logical);
    }

    /**
     * 按事件类型解析标准 Topic（fail-fast）。
     * <p>未知逻辑事件类型直接抛异常，禁止任何默认字符串拼接推导。
     *
     * @param eventType 事件类型（如 VehicleProduceEvent）
     * @return 标准 Topic 名称
     * @throws IllegalArgumentException 未知逻辑事件类型
     */
    public String resolve(String eventType) {
        VmdKafkaLogicalTopic logical = EVENT_TYPE_ROUTES.get(eventType);
        if (logical == null) {
            throw new IllegalArgumentException("未知逻辑事件类型，拒绝路由: " + eventType);
        }
        return topicName(logical);
    }

    /**
     * 按事件类型解析逻辑 Topic（fail-fast）。
     *
     * @param eventType 事件类型
     * @return 逻辑 Topic
     * @throws IllegalArgumentException 未知逻辑事件类型
     */
    public VmdKafkaLogicalTopic logicalTopic(String eventType) {
        VmdKafkaLogicalTopic logical = EVENT_TYPE_ROUTES.get(eventType);
        if (logical == null) {
            throw new IllegalArgumentException("未知逻辑事件类型，拒绝路由: " + eventType);
        }
        return logical;
    }

    /**
     * 判断事件类型是否已登记。
     *
     * @param eventType 事件类型
     * @return 是否已登记
     */
    public boolean isRegistered(String eventType) {
        return EVENT_TYPE_ROUTES.containsKey(eventType);
    }

    /**
     * 安全解析（不抛异常）。
     *
     * @param eventType 事件类型
     * @return 标准 Topic 名称；未登记返回 empty
     */
    public Optional<String> resolveIfRegistered(String eventType) {
        VmdKafkaLogicalTopic logical = EVENT_TYPE_ROUTES.get(eventType);
        return logical == null ? Optional.empty() : Optional.of(topicName(logical));
    }
}
