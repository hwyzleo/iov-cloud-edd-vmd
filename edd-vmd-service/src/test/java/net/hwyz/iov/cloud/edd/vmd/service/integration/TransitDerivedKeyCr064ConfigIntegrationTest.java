package net.hwyz.iov.cloud.edd.vmd.service.integration;

import net.hwyz.iov.cloud.edd.vmd.service.BaseTest;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.DerivedKeyProviderReadinessGate;
import net.hwyz.iov.cloud.framework.security.crypto.KeyProvisioningTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.provider.DerivedKeyProviderRouter;
import net.hwyz.iov.cloud.framework.security.crypto.transit.TransitDerivedKeyHealthIndicator;
import net.hwyz.iov.cloud.framework.security.crypto.transit.TransitDerivedKeyReadiness;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * VMD-DSN-CR-064 配置装配集成测试（真实 dev MySQL，不 mock FW-SEC 组件）。
 * <p>
 * 验证测试环境显式选择 {@code openbao-transit-local-wrap} 后：
 * <ul>
 *   <li>{@link DerivedKeyProviderRouter} 启动静态选中 Transit（非 legacy）；</li>
 *   <li>FW-SEC {@link TransitDerivedKeyReadiness}/{@link TransitDerivedKeyHealthIndicator}
 *       与 {@link KeyProvisioningTemplate} 正常装配；</li>
 *   <li>VMD {@link DerivedKeyProviderReadinessGate} 使用 Transit 聚合且 dev/test 跳过启动探测；</li>
 *   <li>keyprov Health Group 配置含 {@code transitDerivedKey}（主聚合排除由
 *       {@code KeyProvisioningHealthGroupsCustomizerTest} 覆盖）。</li>
 * </ul>
 * 业务 API、DTO、目录与状态机零改动（RD-064-4），仅验证配置绑定与 FW-SEC 接线。
 *
 * @author hwyz_leo
 * @since 2026-10-22
 */
@DisplayName("VMD-DSN-CR-064 Transit local-wrap 配置装配")
class TransitDerivedKeyCr064ConfigIntegrationTest extends BaseTest {

    @Autowired
    private DerivedKeyProviderRouter derivedKeyProviderRouter;

    @Autowired
    private TransitDerivedKeyReadiness transitDerivedKeyReadiness;

    @Autowired
    private TransitDerivedKeyHealthIndicator transitDerivedKeyHealthIndicator;

    @Autowired
    private KeyProvisioningTemplate keyProvisioningTemplate;

    @Autowired
    private DerivedKeyProviderReadinessGate derivedKeyProviderReadinessGate;

    @Autowired
    private Environment environment;

    @Test
    @DisplayName("provider=openbao-transit-local-wrap 时 Router 静态选中 Transit（非 legacy）")
    void routerSelectsTransit() {
        assertEquals("openbao-transit-local-wrap", derivedKeyProviderRouter.providerName());
        assertNotNull(transitDerivedKeyReadiness);
        assertNotNull(transitDerivedKeyHealthIndicator);
        assertNotNull(keyProvisioningTemplate);
    }

    @Test
    @DisplayName("VMD 根派生门禁使用 Transit 聚合；dev/test 跳过启动探测")
    void readinessGateUsesTransit() {
        // 测试环境 require-ready-at-startup=false → SKIPPED（不摘流）
        assertFalse(derivedKeyProviderReadinessGate.isRequireReadyAtStartup());
        assertTrue(derivedKeyProviderReadinessGate.isReady());
        assertTrue(derivedKeyProviderReadinessGate.reason().contains("require-ready-at-startup=false"));
    }

    @Test
    @DisplayName("keyprov Health Group 配置包含 transitDerivedKey 与 derivedKeyProvider")
    void keyprovGroupConfigured() {
        String include = environment.getProperty("management.endpoint.health.group.keyprov.include");
        assertNotNull(include);
        assertTrue(include.contains("transitDerivedKey"), "keyprov 分组应含 transitDerivedKey: " + include);
        assertTrue(include.contains("derivedKeyProvider"));
        assertTrue(include.contains("keyProvisioningCapability"));
    }
}
