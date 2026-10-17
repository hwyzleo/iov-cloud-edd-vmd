package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.health;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.adapter.web.adapter.KeyProvisioningServiceAdapter;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.BusinessKeyPolicyRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.KeyprovProviderReadinessGate;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.KeyprovConstants;
import net.hwyz.iov.cloud.framework.security.crypto.BusinessKeyMaterialTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.resolver.CertificateReferenceDirectory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Keyprov capability 独立健康检查（VMD-DSN-CR-059 §7）。
 * <p>
 * 与 VMD 其他能力解耦：本指示器不进入主/全局 readiness 聚合（由
 * {@link KeyProvisioningHealthGroupsCustomizer} 从 primary group 排除），仅通过独立
 * health group {@code keyprov}（/actuator/health/keyprov）对外呈现，供 VAGW 与发布门禁判断
 * 是否启用 Keyprov 流量。
 * <ul>
 *   <li>enabled=false：状态 DISABLED，视为 UP（能力未装配，非故障）</li>
 *   <li>enabled=true：Controller/Adapter/Facade、V2C 业务策略、framework-security 能力或
 *       必要表结构缺失时 DOWN，禁止返回模拟成功</li>
 * </ul>
 *
 * @author hwyz_leo
 * @since 2026-10-16
 */
@Slf4j
@Component
@ConditionalOnClass(HealthIndicator.class)
public class KeyProvisioningCapabilityHealthIndicator implements HealthIndicator {

    private final ObjectProvider<KeyProvisioningServiceAdapter> adapterProvider;
    private final ObjectProvider<BusinessKeyMaterialTemplate> materialTemplateProvider;
    private final BusinessKeyPolicyRegistry policyRegistry;
    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;
    private final ObjectProvider<CertificateReferenceDirectory> certificateDirectoryProvider;
    private final KeyprovProviderReadinessGate providerReadinessGate;

    @Value("${vmd.business-key.provisioning.keyprov.enabled:false}")
    private boolean keyprovEnabled;

    public KeyProvisioningCapabilityHealthIndicator(ObjectProvider<KeyProvisioningServiceAdapter> adapterProvider,
                                                    ObjectProvider<BusinessKeyMaterialTemplate> materialTemplateProvider,
                                                    BusinessKeyPolicyRegistry policyRegistry,
                                                    ObjectProvider<JdbcTemplate> jdbcTemplateProvider,
                                                    ObjectProvider<CertificateReferenceDirectory> certificateDirectoryProvider,
                                                    KeyprovProviderReadinessGate providerReadinessGate) {
        this.adapterProvider = adapterProvider;
        this.materialTemplateProvider = materialTemplateProvider;
        this.policyRegistry = policyRegistry;
        this.jdbcTemplateProvider = jdbcTemplateProvider;
        this.certificateDirectoryProvider = certificateDirectoryProvider;
        this.providerReadinessGate = providerReadinessGate;
    }

    @Override
    public Health health() {
        if (!keyprovEnabled) {
            return Health.up()
                    .withDetail("state", "DISABLED")
                    .withDetail("note", "vmd.business-key.provisioning.keyprov.enabled=false，Keyprov capability 未装配")
                    .build();
        }
        Health.Builder builder = Health.up().withDetail("state", "READY");
        boolean down = false;

        if (adapterProvider.getIfAvailable() == null) {
            down = down(builder, "adapter", "KeyProvisioningServiceAdapter 缺失");
        }
        if (materialTemplateProvider.getIfAvailable() == null) {
            down = down(builder, "frameworkSecurity", "framework-security 业务密钥能力未装配");
        }
        if (certificateDirectoryProvider.getIfAvailable() == null) {
            down = down(builder, "certificateDirectory", "证书目录 Adapter 未装配（CertificateReferenceDirectory 缺失）");
        }
        if (!providerReadinessGate.isReady()) {
            down = down(builder, "providerReadiness",
                    "business-key Provider capability 未就绪: " + providerReadinessGate.reason());
        }
        Set<String> purposes = policyRegistry.resolvePurposes(KeyprovConstants.BUSINESS_DOMAIN_V2C).orElse(Set.of());
        if (!KeyprovConstants.isCompleteV2cPurposeSet(purposes)) {
            down = down(builder, "policy", "V2C_COMM 业务策略缺失或用途不完整");
        }
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (jdbcTemplate == null) {
            down = down(builder, "schema", "JdbcTemplate 缺失，无法校验开通批次表结构");
        } else {
            try {
                jdbcTemplate.query("SELECT 1 FROM tb_business_key_provisioning LIMIT 1", rs -> null);
            } catch (RuntimeException e) {
                down = down(builder, "schema", "必要表结构缺失: tb_business_key_provisioning");
            }
        }
        return down ? builder.down().withDetail("state", "DOWN").build() : builder.build();
    }

    private boolean down(Health.Builder builder, String key, String reason) {
        builder.withDetail(key, "DOWN").withDetail(key + ".reason", reason);
        return true;
    }
}
