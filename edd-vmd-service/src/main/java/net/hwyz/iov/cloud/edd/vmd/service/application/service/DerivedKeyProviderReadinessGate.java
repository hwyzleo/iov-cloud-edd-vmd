package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.security.crypto.KeyProvisioningTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.transit.TransitDerivedKeyProvider;
import net.hwyz.iov.cloud.framework.security.crypto.transit.TransitDerivedKeyReadiness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 根派生 Provider 就绪门禁（VMD-DSN-CR-061 §10 / VMD-DSN-CR-064 §5）。
 * <p>
 * V2C keyprov 由 {@code KeyProvisioningTemplate.deriveAndWrap} 承接，其能力来自框架
 * {@code crypto.provisioning.enabled} 与 FW-SEC 内置 {@link TransitDerivedKeyProvider}
 * （Transit HMAC v2c-comm-root + 本地证书公钥封装，wire 路径不再依赖
 * {@code /v1/business-key/derived-keys:wrap}）。
 * <p>
 * CR-064：DERIVED_ROOT readiness 从 legacy {@code /v1/business-key/capabilities} 切换为
 * {@link TransitDerivedKeyReadiness} 聚合（OpenBao active/unsealed、AppRole 认证、
 * transit mount/根元数据、HMAC 路径权限、证书 validator、本地 Wrap/JCA/SecureRandom、
 * V2C 策略/derivationVersion）。稳定失败 reason 由 FW-SEC 提供，VMD 只把 keyprov
 * capability 置 DOWN，不自行解释 rootRef 或扩大权限（RD-064-5）。
 * <p>
 * 就绪条件：
 * <ol>
 *   <li>{@link KeyProvisioningTemplate} bean 存在；且</li>
 *   <li>{@code crypto.provisioning.derived-key.require-ready-at-startup=false}，或
 *       framework {@link TransitDerivedKeyReadiness} READY。</li>
 * </ol>
 * 未 READY 时 keyprov Health Group DOWN，禁止 V2C 新流量，且**禁止回退**到随机 Create
 * 或 legacy Provider（Provider 由 FW-SEC Router 启动静态选择，RD-064-3）。
 *
 * @author hwyz_leo
 * @since 2026-10-17
 */
@Slf4j
@Component
public class DerivedKeyProviderReadinessGate {

    private final ObjectProvider<KeyProvisioningTemplate> derivedRootProvider;
    private final ObjectProvider<TransitDerivedKeyReadiness> readinessProvider;

    /**
     * 是否要求根派生 Provider 在启动时 READY（仓内安全默认 true；dev/test 显式关闭）。
     */
    @Value("${crypto.provisioning.derived-key.require-ready-at-startup:true}")
    private boolean requireReadyAtStartup = true;

    public DerivedKeyProviderReadinessGate(ObjectProvider<KeyProvisioningTemplate> derivedRootProvider,
                                           ObjectProvider<TransitDerivedKeyReadiness> readinessProvider) {
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
        TransitDerivedKeyReadiness readiness = readinessProvider.getIfAvailable();
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
        TransitDerivedKeyReadiness readiness = readinessProvider.getIfAvailable();
        if (readiness == null) {
            return "TransitDerivedKeyReadiness 未装配（derived-key.provider 未选择 openbao-transit-local-wrap）";
        }
        String reason = readiness.notReadyReason();
        return reason != null ? reason : "TransitDerivedKey capability READY（openbao-transit-local-wrap）";
    }

    /**
     * 是否强制在启动/健康检查时要求 Provider READY。
     */
    public boolean isRequireReadyAtStartup() {
        return requireReadyAtStartup;
    }
}
