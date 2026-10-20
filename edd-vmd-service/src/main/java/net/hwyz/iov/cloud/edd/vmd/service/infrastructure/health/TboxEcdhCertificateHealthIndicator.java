package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.health;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.BoundDeviceIdentityResolver;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.DeviceCertificateIdentityPolicy;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.IssuedCertificateVerifier;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.certificateprofile.CertificateProfileRegistry;
import net.hwyz.iov.cloud.edd.vmd.service.application.service.certificateprofile.VmdCertificateProfile;
import net.hwyz.iov.cloud.framework.security.crypto.CertEnrollmentTemplate;
import net.hwyz.iov.cloud.framework.security.crypto.KeyprovCertificateProfileCapability;
import net.hwyz.iov.cloud.framework.security.crypto.KeyprovReadiness;
import net.hwyz.iov.cloud.framework.security.crypto.TboxDeviceCertificateValidator;
import net.hwyz.iov.cloud.framework.security.crypto.resolver.CertificateReferenceDirectory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 逻辑 TBOX ECDH-ES v1 证书能力健康检查（VMD-DSN-CR-063 §12）。
 * <p>
 * 聚合 {@code VMD_TBOX_ECDH_CERT_READY} 八项子条件：
 * <ol>
 *   <li>profileRegistryReady——逻辑 Profile 注册表已装配且 v1 已注册启用；</li>
 *   <li>csrParserAndPolicyReady——CSR 解析与双用途策略可执行；</li>
 *   <li>boundIdentityResolverReady——active 绑定/权威 UID 解析器；</li>
 *   <li>frameworkEnrollmentProfileRouteReady——framework enrollment 路由声明逻辑 Profile 映射；</li>
 *   <li>fwSecTboxProfileCapabilityReady——FW-SEC CR-017 validator + ECDH-ES v1 capability；</li>
 *   <li>issuedCertificateVerifierReady——签发后复验器；</li>
 *   <li>certificateReferenceDirectoryReady——证书目录可解析材料；</li>
 *   <li>leafCertificateDigestSha256Ready——leaf digest 列与 SHA-256 计算可用。</li>
 * </ol>
 * {@code VMD_KEYPROV_READY = VMD_TBOX_ECDH_CERT_READY AND CR062_KEYPROV_READY} 由 keyprov Health Group
 * 聚合（本指示器加入 {@code keyprov} group，与 CR-062 指示器按最差状态取 AND）。
 * enabled=false 时状态 DISABLED（能力未启用，非故障），新 ECDH-ES 请求路径由
 * {@code TboxEcdhCertificateEligibilityGate} 在请求面关闭。
 *
 * @author hwyz_leo
 * @since 2026-10-20
 */
@Slf4j
@Component
@ConditionalOnClass(HealthIndicator.class)
public class TboxEcdhCertificateHealthIndicator implements HealthIndicator {

    private final CertificateProfileRegistry certificateProfileRegistry;
    private final DeviceCertificateIdentityPolicy deviceCertificateIdentityPolicy;
    private final BoundDeviceIdentityResolver boundDeviceIdentityResolver;
    private final IssuedCertificateVerifier issuedCertificateVerifier;
    private final ObjectProvider<CertEnrollmentTemplate> certEnrollmentTemplateProvider;
    private final ObjectProvider<TboxDeviceCertificateValidator> tboxValidatorProvider;
    private final ObjectProvider<KeyprovReadiness> keyprovReadinessProvider;
    private final ObjectProvider<CertificateReferenceDirectory> certificateDirectoryProvider;
    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;

    @Value("${vmd.certificate.profiles.tbox-device-ecdh-es-p256-v1.enabled:false}")
    private boolean ecdhV1Enabled;

    public TboxEcdhCertificateHealthIndicator(CertificateProfileRegistry certificateProfileRegistry,
                                              DeviceCertificateIdentityPolicy deviceCertificateIdentityPolicy,
                                              BoundDeviceIdentityResolver boundDeviceIdentityResolver,
                                              IssuedCertificateVerifier issuedCertificateVerifier,
                                              ObjectProvider<CertEnrollmentTemplate> certEnrollmentTemplateProvider,
                                              ObjectProvider<TboxDeviceCertificateValidator> tboxValidatorProvider,
                                              ObjectProvider<KeyprovReadiness> keyprovReadinessProvider,
                                              ObjectProvider<CertificateReferenceDirectory> certificateDirectoryProvider,
                                              ObjectProvider<JdbcTemplate> jdbcTemplateProvider) {
        this.certificateProfileRegistry = certificateProfileRegistry;
        this.deviceCertificateIdentityPolicy = deviceCertificateIdentityPolicy;
        this.boundDeviceIdentityResolver = boundDeviceIdentityResolver;
        this.issuedCertificateVerifier = issuedCertificateVerifier;
        this.certEnrollmentTemplateProvider = certEnrollmentTemplateProvider;
        this.tboxValidatorProvider = tboxValidatorProvider;
        this.keyprovReadinessProvider = keyprovReadinessProvider;
        this.certificateDirectoryProvider = certificateDirectoryProvider;
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    @Override
    public Health health() {
        if (!ecdhV1Enabled) {
            return Health.up()
                    .withDetail("state", "DISABLED")
                    .withDetail("note",
                            "vmd.certificate.profiles.tbox-device-ecdh-es-p256-v1.enabled=false，逻辑Profile未启用（新ECDH-ES请求路径关闭，RD-063-5/6）")
                    .build();
        }
        Health.Builder builder = Health.up().withDetail("state", "READY");
        boolean down = false;

        // 1. profileRegistryReady
        if (certificateProfileRegistry == null
                || !certificateProfileRegistry.isEnabled(VmdCertificateProfile.TBOX_DEVICE_ECDH_ES_P256_V1)) {
            down = down(builder, "profileRegistry",
                    "逻辑Profile注册表未装配或 v1 未启用（CertificateProfileRegistry/TBOX_DEVICE_ECDH_ES_P256_V1）");
        }

        // 2. csrParserAndPolicyReady
        if (deviceCertificateIdentityPolicy == null) {
            down = down(builder, "csrParserAndPolicy", "CSR 双用途策略未装配（DeviceCertificateIdentityPolicy 缺失）");
        }

        // 3. boundIdentityResolverReady
        if (boundDeviceIdentityResolver == null) {
            down = down(builder, "boundIdentityResolver", "active绑定/权威UID解析器未装配");
        }

        // 4. frameworkEnrollmentProfileRouteReady
        if (certEnrollmentTemplateProvider.getIfAvailable() == null
                || !KeyprovCertificateProfileCapability.supports(
                KeyprovCertificateProfileCapability.TBOX_DEVICE_CERT_PROFILE_ECDH_ES_P256_V1, true)) {
            down = down(builder, "frameworkEnrollmentRoute",
                    "framework enrollment 路由或逻辑Profile capability 未就绪（TBOX_DEVICE_ECDH_ES_P256_V1）");
        }

        // 5. fwSecTboxProfileCapabilityReady
        KeyprovReadiness keyprovReadiness = keyprovReadinessProvider.getIfAvailable();
        if (tboxValidatorProvider.getIfAvailable() == null
                || keyprovReadiness == null || !keyprovReadiness.keyprovEcdhEsV1Ready()) {
            down = down(builder, "fwSecTboxProfileCapability",
                    "FW-SEC CR-017 validator 未装配或 ECDH-ES v1 capability 未就绪（KeyprovReadiness.keyprovEcdhEsV1Ready）");
        }

        // 6. issuedCertificateVerifierReady
        if (issuedCertificateVerifier == null) {
            down = down(builder, "issuedCertificateVerifier", "签发后复验器未装配（IssuedCertificateVerifier 缺失）");
        }

        // 7. certificateReferenceDirectoryReady
        if (certificateDirectoryProvider.getIfAvailable() == null) {
            down = down(builder, "certificateReferenceDirectory", "证书目录未装配（CertificateReferenceDirectory 缺失）");
        }

        // 8. leafCertificateDigestSha256Ready
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getIfAvailable();
        if (jdbcTemplate == null) {
            down = down(builder, "leafCertificateDigest", "JdbcTemplate 缺失，无法校验 leaf digest 列");
        } else {
            try {
                jdbcTemplate.query("SELECT leaf_certificate_digest, profile_validated_at FROM tb_veh_certificate LIMIT 1",
                        rs -> {
                        });
            } catch (RuntimeException e) {
                down = down(builder, "leafCertificateDigest",
                        "tb_veh_certificate 缺少 leaf_certificate_digest/profile_validated_at 列（V62 未迁移）");
            }
        }

        return down ? builder.down().withDetail("state", "DOWN").build() : builder.build();
    }

    private boolean down(Health.Builder builder, String key, String reason) {
        builder.withDetail(key, "DOWN").withDetail(key + ".reason", reason);
        return true;
    }
}
