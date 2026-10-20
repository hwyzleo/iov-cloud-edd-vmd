package net.hwyz.iov.cloud.edd.vmd.service.application.service.certificateprofile;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.service.common.exception.CertificateProfileNotAllowedException;
import net.hwyz.iov.cloud.framework.security.crypto.model.CertificateProfile;
import net.hwyz.iov.cloud.framework.security.crypto.model.DeviceCertificateProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 默认证书 Profile 注册表（VMD-DSN-CR-063 §4.1）。
 * <p>
 * 固定映射：
 * <pre>
 * VMD TBOX_DEVICE_ECDH_ES_P256_V1
 *   → FW DeviceCertificateProfile.TBOX_DEVICE_ECDH_ES_P256_V1（CR-017 validator）
 *   → framework CertificateProfile（签发申请，usage=DIGITAL_SIGNATURE,KEY_AGREEMENT）
 *   → deviceCategory=TBOX → keyprovEligible=true
 * </pre>
 * 存量 {@code TBOX_TSP_CLIENT} 注册为 legacy sign-only（keyprovEligible=false，mTLS 兼容）。
 * 配置只允许逻辑开关与逻辑→框架 Profile 名映射，禁止出现 Provisioner 名称/Secret/模板路径。
 *
 * @author hwyz_leo
 * @since 2026-10-20
 */
@Slf4j
@Component
public class DefaultCertificateProfileRegistry implements CertificateProfileRegistry {

    /** 存量 legacy Profile 签发申请固定用法（与 CR-044/054 现状一致） */
    private static final String LEGACY_USAGE = "DIGITAL_SIGNATURE";

    /** 逻辑 TBOX ECDH-ES v1 Profile 签发申请用法（DS + KA，CR-063 §4.2/§6） */
    private static final String ECDH_USAGE = "DIGITAL_SIGNATURE,KEY_AGREEMENT";

    private final Map<VmdCertificateProfile, RegisteredCertificateProfile> profiles;

    public DefaultCertificateProfileRegistry(
            @Value("${vmd.certificate.profiles.tbox-device-ecdh-es-p256-v1.enabled:false}") boolean ecdhV1Enabled,
            @Value("${vmd.certificate.profiles.tbox-device-ecdh-es-p256-v1.framework-profile:TBOX_DEVICE_ECDH_ES_P256_V1}")
            String ecdhV1FrameworkProfile) {
        DeviceCertificateProfile fwProfile = parseFrameworkProfile(ecdhV1FrameworkProfile);
        Map<VmdCertificateProfile, RegisteredCertificateProfile> built = new LinkedHashMap<>();
        built.put(VmdCertificateProfile.TBOX_TSP_CLIENT,
                new RegisteredCertificateProfile(
                        VmdCertificateProfile.TBOX_TSP_CLIENT,
                        null,
                        new CertificateProfile("TBOX_TSP_CLIENT", "TBOX_TSP_CLIENT",
                                CertificateProfile.SubjectType.DEVICE_IDENTITY, "EC", LEGACY_USAGE),
                        "TBOX", false, true));
        built.put(VmdCertificateProfile.TBOX_DEVICE_ECDH_ES_P256_V1,
                new RegisteredCertificateProfile(
                        VmdCertificateProfile.TBOX_DEVICE_ECDH_ES_P256_V1,
                        fwProfile,
                        new CertificateProfile("TBOX_DEVICE_ECDH_ES_P256_V1", "TBOX_DEVICE_ECDH_ES_P256_V1",
                                CertificateProfile.SubjectType.DEVICE_IDENTITY, "EC", ECDH_USAGE),
                        "TBOX", true, ecdhV1Enabled));
        this.profiles = Map.copyOf(built);
        log.info("证书Profile注册表就绪: tbox-ecdh-es-v1-enabled={}, framework-profile={}",
                ecdhV1Enabled, fwProfile.name());
    }

    @Override
    public RegisteredCertificateProfile requireEnabled(VmdCertificateProfile profile) {
        if (profile == null) {
            throw new CertificateProfileNotAllowedException(null);
        }
        RegisteredCertificateProfile registered = profiles.get(profile);
        if (registered == null || !registered.enabled()) {
            throw new CertificateProfileNotAllowedException(profile.name());
        }
        return registered;
    }

    @Override
    public RegisteredCertificateProfile requireEnabled(String externalProfile) {
        return VmdCertificateProfile.parse(externalProfile)
                .map(this::requireEnabled)
                .orElseThrow(() -> new CertificateProfileNotAllowedException(externalProfile));
    }

    @Override
    public boolean isEnabled(VmdCertificateProfile profile) {
        if (profile == null) {
            return false;
        }
        RegisteredCertificateProfile registered = profiles.get(profile);
        return registered != null && registered.enabled();
    }

    @Override
    public Set<VmdCertificateProfile> registeredProfiles() {
        return profiles.keySet();
    }

    /**
     * 严格解析配置的 framework Profile 名；未知值启动期 fail-fast（不得把外部字符串当 Provisioner 名）。
     */
    private DeviceCertificateProfile parseFrameworkProfile(String configured) {
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException("vmd.certificate.profiles.tbox-device-ecdh-es-p256-v1.framework-profile 为空");
        }
        try {
            return DeviceCertificateProfile.valueOf(configured.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "framework-profile 不是受治理的 FW-SEC DeviceCertificateProfile: " + configured, e);
        }
    }
}
