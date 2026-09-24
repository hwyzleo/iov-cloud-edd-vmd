package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

/**
 * VMD Kafka 逻辑 Topic 目录（Kafka Topic 目录 SSOT）
 * <p>
 * VMD-DSN-CR-051：四个 EDD-VMD 相关 Topic 的逻辑名，与实际 Topic 名称一一对应，
 * 值来自类型化配置 {@link net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.VmdKafkaTopicProperties}。
 * <p>
 * 注意：{@code part-binding-changed} 对应 {@code vmd.vehcile-part-binding.changed} 中的
 * {@code vehcile} 拼写为 Kafka Topic 目录治理值（RD-051-6），代码内禁止私自纠正。
 *
 * @author hwyz_leo
 */
public enum VmdKafkaLogicalTopic {

    /**
     * OTA 车辆软件观测事件（Consumer，IOV-OTA 管理）
     */
    INVENTORY_OBSERVED("inventory-observed", VmdKafkaTopicRole.CONSUMER_ONLY),

    /**
     * 车辆-零件绑定变更事件（Producer，VMD 幂等创建）
     */
    PART_BINDING_CHANGED("part-binding-changed", VmdKafkaTopicRole.PRODUCER_OWNED),

    /**
     * 车辆生产事件（Producer，VMD 幂等创建，经 Outbox→Relay 发布/补发）
     */
    VEHICLE_PRODUCE("vehicle-produce", VmdKafkaTopicRole.PRODUCER_OWNED),

    /**
     * 车辆软件清单变更事件（Producer，VMD 幂等创建，经 Outbox→Relay 发布）
     */
    SOFTWARE_INVENTORY_CHANGED("software-inventory-changed", VmdKafkaTopicRole.PRODUCER_OWNED);

    private final String configKey;
    private final VmdKafkaTopicRole role;

    VmdKafkaLogicalTopic(String configKey, VmdKafkaTopicRole role) {
        this.configKey = configKey;
        this.role = role;
    }

    /**
     * 配置键（{@code vmd.kafka.topics.*} 下的段名）。
     */
    public String configKey() {
        return configKey;
    }

    /**
     * Topic 角色（所有权）。
     */
    public VmdKafkaTopicRole role() {
        return role;
    }
}
