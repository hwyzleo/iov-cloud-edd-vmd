package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.security.crypto.KeyProvisioningTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.client.KmsBusinessKeyProviderReadiness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 根派生 Provider 就绪门禁（VMD-DSN-CR-061 §10）。
 * <p>
 * V2C keyprov 由 {@code KeyProvisioningTemplate.deriveAndWrap} 承接，其能力来自框架
 * {@code crypto.provisioning.enabled} 与 Transit/HSM 受治理根 {@code v2c-comm-root}
 * （wire 路径 {@code /v1/business-key/derived-keys:wrap}，与 business-key mount 同源）。
 * <p>
 * 就绪条件：
 * <ol>
 *   <li>{@link KeyProvisioningTemplate} bean 存在；且</li>
 *   <li>{@code crypto.provisioning.derived-key.require-ready-at-startup=false}，或
 *       framework {@link KmsBusinessKeyProviderReadiness} READY。</li>
 * </ol>
 * 未 READY 时 keyprov Health Group DOWN，禁止 V2C 新流量，且**禁止回退**到随机 Create。
 *
 * @author hwyz_leo
 * @since 2026-10-17
 */
@Slf4j
@Component
public class DerivedKeyProviderReadinessGate {

    private final ObjectProvider<KeyProvisioningTemplate> derivedRootProvider;
    private final ObjectProvider<KmsBusinessKeyProviderReadiness> readinessProvider;

    /**
     * 是否要求根派生 Provider 在启动时 READY（仓内安全默认 true；dev/test 显式关闭）。
     */
    @Value("${crypto.provisioning.derived-key.require-ready-at-startup:true}")
    private boolean requireReadyAtStartup = true;

    public DerivedKeyProviderReadinessGate(ObjectProvider<KeyProvisioningTemplate> derivedRootProvider,
                                           ObjectProvider<KmsBusinessKeyProviderReadiness> readinessProvider) {
        this.derivedRootProvider = derivedRootProvider;
        this.readinessProvider = readinessProvider;
    }

    /**
     * 根派生能力是否 READY（fail-closed）。
     */
    public boolean isReady() {
        if (derivedRootProvider.getIfAvailable() == null) {
            return false;
        }
        if (!requireReadyAtStartup) {
            return true;
        }
        KmsBusinessKeyProviderReadiness readiness = readinessProvider.getIfAvailable();
        return readiness != null && readiness.isReady();
    }

    /**
     * 未就绪原因（用于日志/健康明细，不泄露 Secret）。
     */
    public String reason() {
        if (derivedRootProvider.getIfAvailable() == null) {
            return "KeyProvisioningTemplate 未装配（crypto.provisioning.enabled=false）";
        }
        if (!requireReadyAtStartup) {
            return "ready（require-ready-at-startup=false，跳过 Provider 探测）";
        }
        KmsBusinessKeyProviderReadiness readiness = readinessProvider.getIfAvailable();
        if (readiness == null) {
            return "KmsBusinessKeyProviderReadiness 未装配（crypto.kms.business-key.enabled=false）";
        }
        return readiness.getReason();
    }

    /**
     * 是否强制在启动/健康检查时要求 Provider READY。
     */
    public boolean isRequireReadyAtStartup() {
        return requireReadyAtStartup;
    }
}