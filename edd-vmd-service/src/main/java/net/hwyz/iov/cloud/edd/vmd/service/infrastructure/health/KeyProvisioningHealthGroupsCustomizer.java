package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.health;

import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.health.AdditionalHealthEndpointPath;
import org.springframework.boot.actuate.health.HealthEndpointGroup;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.boot.actuate.health.HealthEndpointGroupsPostProcessor;
import org.springframework.boot.actuate.health.HttpCodeStatusMapper;
import org.springframework.boot.actuate.health.StatusAggregator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 将 Keyprov capability 从 Actuator 主健康聚合中排除（VMD-DSN-CR-059 §7）。
 * <p>
 * 主/全局 readiness 与 liveness 只反映 VMD 核心能力；Keyprov 独立能力故障不得摘流整个 Pod。
 * 该能力仍通过独立 health group {@code keyprov}（/actuator/health/keyprov）暴露。
 *
 * @author hwyz_leo
 * @since 2026-10-16
 */
@Component
@ConditionalOnClass(HealthEndpointGroupsPostProcessor.class)
public class KeyProvisioningHealthGroupsCustomizer implements HealthEndpointGroupsPostProcessor {

    /**
     * Keyprov capability 健康指示器注册名
     */
    public static final String KEYPROV_INDICATOR = "keyProvisioningCapability";

    @Override
    public HealthEndpointGroups postProcessHealthEndpointGroups(HealthEndpointGroups groups) {
        if (groups == null) {
            return null;
        }
        return new HealthEndpointGroups() {
            @Override
            public HealthEndpointGroup getPrimary() {
                return excludingKeyprov(groups.getPrimary());
            }

            @Override
            public Set<String> getNames() {
                return groups.getNames();
            }

            @Override
            public HealthEndpointGroup get(String name) {
                return groups.get(name);
            }
        };
    }

    private HealthEndpointGroup excludingKeyprov(HealthEndpointGroup group) {
        if (group == null) {
            return null;
        }
        return new HealthEndpointGroup() {
            @Override
            public boolean isMember(String name) {
                return !KEYPROV_INDICATOR.equals(name) && group.isMember(name);
            }

            @Override
            public boolean showComponents(SecurityContext securityContext) {
                return group.showComponents(securityContext);
            }

            @Override
            public boolean showDetails(SecurityContext securityContext) {
                return group.showDetails(securityContext);
            }

            @Override
            public StatusAggregator getStatusAggregator() {
                return group.getStatusAggregator();
            }

            @Override
            public HttpCodeStatusMapper getHttpCodeStatusMapper() {
                return group.getHttpCodeStatusMapper();
            }

            @Override
            public AdditionalHealthEndpointPath getAdditionalPath() {
                return group.getAdditionalPath();
            }
        };
    }
}
