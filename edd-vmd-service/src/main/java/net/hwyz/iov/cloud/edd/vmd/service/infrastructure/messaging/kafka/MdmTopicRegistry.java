package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config.VmdKafkaTopicProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

/**
 * MDM 投影 → Topic 显式注册表（VMD-DSN-CR-052，RD-052-3）
 * <p>
 * 维护 {@link MdmProjectionType} → 配置键 → 实际 Topic → Listener 容器 id 的显式映射。
 * Listener、预检、健康检查与测试统一经本注册表取值，未知投影类型 fail-fast，
 * 禁止按实体类名或前缀自动推导 Topic。
 * <p>
 * 注册表为 SSOT 入口，实际 Topic 名称由 {@link VmdKafkaTopicProperties} 提供（目录对齐）。
 *
 * @author hwyz_leo
 */
@Component
@RequiredArgsConstructor
public class MdmTopicRegistry {

    private final VmdKafkaTopicProperties topicProperties;

    /**
     * 按投影类型读取实际消费 Topic 名称（fail-fast）。
     *
     * @param projection 投影类型
     * @return 实际 Topic 名称
     */
    public String topicName(MdmProjectionType projection) {
        return topicProperties.topic(projection);
    }

    /**
     * 按投影类型读取 Listener 容器 id。
     *
     * @param projection 投影类型
     * @return 容器 id
     */
    public String consumerId(MdmProjectionType projection) {
        return projection.consumerId();
    }

    /**
     * 判断配置键是否已登记。
     *
     * @param configKey 配置键（如 {@code car-line}）
     * @return 是否已登记
     */
    public boolean isRegistered(String configKey) {
        return Arrays.stream(MdmProjectionType.values())
                .anyMatch(p -> p.configKey().equals(configKey));
    }

    /**
     * 按配置键解析投影类型（fail-fast）。
     *
     * @param configKey 配置键
     * @return 投影类型
     * @throws IllegalArgumentException 未知配置键
     */
    public MdmProjectionType fromConfigKey(String configKey) {
        return Arrays.stream(MdmProjectionType.values())
                .filter(p -> p.configKey().equals(configKey))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知 MDM 投影配置键，拒绝解析: " + configKey));
    }

    /**
     * 安全解析（不抛异常）。
     *
     * @param configKey 配置键
     * @return 投影类型；未登记返回 empty
     */
    public Optional<MdmProjectionType> findByConfigKey(String configKey) {
        return Arrays.stream(MdmProjectionType.values())
                .filter(p -> p.configKey().equals(configKey))
                .findFirst();
    }
}
