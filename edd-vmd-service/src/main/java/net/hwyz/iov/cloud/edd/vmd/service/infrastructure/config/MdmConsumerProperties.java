package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * VMD MDM 消费 Topic 预检 / 启动门禁配置（VMD-DSN-CR-052）
 * <p>
 * 控制 11 个 MDM 消费 Topic 启动预检的失败策略与 Listener 启动门禁：
 * <ul>
 *   <li>fail-fast：生产环境默认 true。任一必需 MDM Topic 缺失 / 不可达 / Read ACL 不足时，
 *       使对应投影 readiness=DOWN 并拒绝启动消费；配置为 false 表示允许降级启动，
 *       但健康状态仍为 DOWN/DEGRADED 且后台持续重试探测。</li>
 *   <li>auto-startup：默认 true（保持既有行为，Listener 由 Spring 自动启动）；
 *       配置为 false 时，MDM Listener 仅在预检全部通过（且 Bootstrap 基线完成后）
 *       由 {@link net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka.MdmConsumerStartupCoordinator}
 *       显式启动。</li>
 *   <li>retry-interval-ms：预检失败后的后台重试探测间隔。</li>
 * </ul>
 *
 * @author hwyz_leo
 */
@Data
@Component
@ConfigurationProperties(prefix = "vmd.kafka.mdm-consumer")
public class MdmConsumerProperties {

    /**
     * 预检失败是否 fail-fast（默认 true）。
     */
    private boolean failFast = true;

    /**
     * MDM Listener 是否由 Spring 自动启动（默认 true）；false 表示由预检门禁协调启动。
     */
    private boolean autoStartup = true;

    /**
     * 预检失败后的后台重试探测间隔（毫秒，默认 60s）。
     */
    private long retryIntervalMs = 60_000L;
}
