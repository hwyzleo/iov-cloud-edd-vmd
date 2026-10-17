package net.hwyz.iov.cloud.edd.vmd.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.security.crypto.client.KmsBusinessKeyProviderReadiness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * business-key Provider 就绪门禁（VMD-DSN-CR-060 RD-060-9 / §7）。
 * <p>
 * 封装 framework {@link KmsBusinessKeyProviderReadiness}，为 VMD 提供“Provider capability 是否 READY”
 * 的统一判定。只有 READY Provider 的结构化 {@code BUSINESS_KEY_NOT_FOUND} 才可收敛为业务幂等成功；
 * capability 未启用或未就绪时一律视为未就绪，禁止把 Provider 缺失伪装成业务成功。
 *
 * @author hwyz_leo
 * @since 2026-10-17
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeyprovProviderReadinessGate {

    private final ObjectProvider<KmsBusinessKeyProviderReadiness> readinessProvider;

    /**
     * Provider capability 是否 READY（未装配视为未就绪，fail-closed）
     */
    public boolean isReady() {
        KmsBusinessKeyProviderReadiness readiness = readinessProvider.getIfAvailable();
        return readiness != null && readiness.isReady();
    }

    /**
     * 未就绪原因（用于日志/健康明细，不泄露 Secret）
     */
    public String reason() {
        KmsBusinessKeyProviderReadiness readiness = readinessProvider.getIfAvailable();
        if (readiness == null) {
            return "KmsBusinessKeyProviderReadiness 未装配（crypto.kms.business-key.enabled=false）";
        }
        return readiness.getReason();
    }
}
