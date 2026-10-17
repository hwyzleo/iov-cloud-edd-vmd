package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.health;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.DerivedKeyProviderReadinessGate;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

/**
 * 根派生 Provider 健康指示（VMD-DSN-CR-061 §10 / FW-SEC-DSN-CR-013 §8）。
 * <p>
 * 代表 Transit/HSM 受治理根 {@code v2c-comm-root} 与 {@code deriveAndWrap} 能力：
 * V2C keyprov 的唯一材料能力来源。类名剥离 {@code HealthIndicator} 后缀后注册为
 * {@code derivedKeyProvider}，进入独立 keyprov health group 并从主聚合排除
 * （见 {@link KeyProvisioningHealthGroupsCustomizer}）。
 * <ul>
 *   <li>根派生门面缺失或 Provider READY 要求未满足 → DOWN</li>
 *   <li>未强制 READY（dev/test）→ UP，标注 SKIPPED</li>
 * </ul>
 *
 * @author hwyz_leo
 * @since 2026-10-17
 */
@Slf4j
@Component
@ConditionalOnClass(HealthIndicator.class)
public class DerivedKeyProviderHealthIndicator implements HealthIndicator {

    private final DerivedKeyProviderReadinessGate readinessGate;

    public DerivedKeyProviderHealthIndicator(DerivedKeyProviderReadinessGate readinessGate) {
        this.readinessGate = readinessGate;
    }

    @Override
    public Health health() {
        if (!readinessGate.isRequireReadyAtStartup()) {
            return Health.up()
                    .withDetail("state", "SKIPPED")
                    .withDetail("note", "require-ready-at-startup=false，未强制根派生 Provider 探测")
                    .withDetail("reason", readinessGate.reason())
                    .build();
        }
        if (!readinessGate.isReady()) {
            return Health.down()
                    .withDetail("state", "DOWN")
                    .withDetail("reason", readinessGate.reason())
                    .build();
        }
        return Health.up()
                .withDetail("state", "READY")
                .withDetail("reason", readinessGate.reason())
                .build();
    }
}