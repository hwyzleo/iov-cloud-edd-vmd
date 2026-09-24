package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

/**
 * MDM 投影类型（VMD-DSN-CR-052，RD-052-3 显式投影注册表语义键）
 * <p>
 * 与 Kafka Topic 目录中 11 个 EDD-MDM 消费 Topic 一一对应，
 * 值来自类型化配置 {@link net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.VmdKafkaTopicProperties#mdm}。
 * Listener / 预检 / 指标 / 测试统一引用本枚举与注册表，禁止按实体类名或前缀自动推导 Topic。
 * <p>
 * 全部 11 个 Topic 均为 CONSUMER_ONLY（生命周期归 EDD-MDM，VMD 只预检和消费，绝不创建）。
 * {@link ConsumerIds} 为 Listener 容器 id 常量表，供 {@code @KafkaListener(id=...)} 编译期引用，
 * 保证容器 id 与注册表、启动协调器保持一致（单一事实来源）。
 *
 * @author hwyz_leo
 */
public enum MdmProjectionType {

    /**
     * 品牌只读投影（Product MDM 子域）
     */
    BRAND("brand", "mdm.brand", ConsumerIds.BRAND),

    /**
     * 车系只读投影（Product MDM 子域）
     */
    CAR_LINE("car-line", "mdm.car-line", ConsumerIds.CAR_LINE),

    /**
     * 配置只读投影（Product MDM 子域）
     */
    CONFIGURATION("configuration", "mdm.configuration", ConsumerIds.CONFIGURATION),

    /**
     * 车型只读投影（Product MDM 子域）
     */
    MODEL("model", "mdm.model", ConsumerIds.MODEL),

    /**
     * 选项值只读投影（Product MDM 子域）
     */
    OPTION_CODE("option-code", "mdm.option-code", ConsumerIds.OPTION_CODE),

    /**
     * 选项族只读投影（Product MDM 子域）
     */
    OPTION_FAMILY("option-family", "mdm.option-family", ConsumerIds.OPTION_FAMILY),

    /**
     * 零件类型只读投影（Part 子域）
     */
    PART("part", "mdm.part", ConsumerIds.PART),

    /**
     * 工厂只读投影（Plant 子域）
     */
    PLANT("plant", "mdm.plant", ConsumerIds.PLANT),

    /**
     * 平台只读投影（Product MDM 子域）
     */
    PLATFORM("platform", "mdm.platform", ConsumerIds.PLATFORM),

    /**
     * 版本（Variant）只读投影（Product MDM 子域）
     */
    VARIANT("variant", "mdm.variant", ConsumerIds.VARIANT),

    /**
     * 车载节点只读投影（EEAD 子域）
     */
    VEHICLE_NODE("vehicle-node", "mdm.vehicle-node", ConsumerIds.VEHICLE_NODE);

    /**
     * MDM 消费 Listener 容器 id 常量表（供 @KafkaListener(id=...) 编译期引用）。
     */
    public interface ConsumerIds {
        /** Brand Listener 容器 id */
        String BRAND = "mdm-brand-consumer";
        /** CarLine Listener 容器 id */
        String CAR_LINE = "mdm-car-line-consumer";
        /** Configuration Listener 容器 id */
        String CONFIGURATION = "mdm-configuration-consumer";
        /** Model Listener 容器 id */
        String MODEL = "mdm-model-consumer";
        /** OptionCode Listener 容器 id */
        String OPTION_CODE = "mdm-option-code-consumer";
        /** OptionFamily Listener 容器 id */
        String OPTION_FAMILY = "mdm-option-family-consumer";
        /** Part Listener 容器 id */
        String PART = "mdm-part-consumer";
        /** Plant Listener 容器 id */
        String PLANT = "mdm-plant-consumer";
        /** Platform Listener 容器 id */
        String PLATFORM = "mdm-platform-consumer";
        /** Variant Listener 容器 id */
        String VARIANT = "mdm-variant-consumer";
        /** VehicleNode Listener 容器 id */
        String VEHICLE_NODE = "mdm-vehicle-node-consumer";
    }

    private final String configKey;
    private final String directoryTopic;
    private final String consumerId;

    MdmProjectionType(String configKey, String directoryTopic, String consumerId) {
        this.configKey = configKey;
        this.directoryTopic = directoryTopic;
        this.consumerId = consumerId;
    }

    /**
     * 配置键（{@code vmd.kafka.topics.mdm.*} 下的段名）。
     */
    public String configKey() {
        return configKey;
    }

    /**
     * Kafka Topic 目录标准值（代码默认值；生产经 Nacos 覆盖，逐字符一致由配置测试固化）。
     */
    public String directoryTopic() {
        return directoryTopic;
    }

    /**
     * 对应 @KafkaListener 的容器 id（供启动协调器按投影启停容器）。
     */
    public String consumerId() {
        return consumerId;
    }
}
