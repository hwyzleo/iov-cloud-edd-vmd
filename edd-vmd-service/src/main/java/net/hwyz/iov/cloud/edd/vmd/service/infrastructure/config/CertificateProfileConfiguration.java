package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.config;

import net.hwyz.iov.cloud.framework.security.crypto.model.CertificateProfile;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 证书 Profile 框架 Bean 注册（VMD-DSN-CR-063 §6 补漏）
 * <p>
 * FW-SEC {@code CryptoAutoConfiguration.certEnrollmentTemplate(List<CertificateProfile>)}
 * 把 Spring 上下文中所有 {@link CertificateProfile} 类型 Bean 收集为签发允许名单
 * （{@code DefaultCertEnrollmentTemplate.allowedProfiles}），框架自身仅注册
 * {@code TBOX_TSP_CLIENT}。若应用侧不注册 {@code TBOX_DEVICE_ECDH_ES_P256_V1}，
 * 签发申请会在 {@code validateProfile} 处抛
 * {@code CertificateProfileNotAllowedException: Certificate profile not allowed}。
 * <p>
 * 取值必须与 {@code DefaultCertificateProfileRegistry} 中
 * {@code RegisteredCertificateProfile.applyProfile()}（name/pkiProfileId/subjectType/
 * allowedAlgorithm/usage = TBOX_DEVICE_ECDH_ES_P256_V1 / DEVICE_IDENTITY / EC /
 * DIGITAL_SIGNATURE,KEY_AGREEMENT）保持一致；逻辑开关仍由注册表 {@code enabled} 治理，
 * 禁用时注册表在请求侧 fail-closed，不会到达框架。
 *
 * @author hwyz_leo
 * @since 2026-10-20
 */
@Configuration
public class CertificateProfileConfiguration {

    /**
     * TBOX ECDH-ES P256 v1 设备证书 Profile 框架 Bean
     * <p>
     * 与框架自带的 {@code tboxTspClientCertificateProfile()} 同构：仅声明允许名单，
     * 物理 Provisioner 路由由 FW-SEC {@code crypto.pki.step-ca.profile-policy} 配置治理。
     */
    @Bean
    public CertificateProfile tboxDeviceEcdhEsP256V1CertificateProfile() {
        return new CertificateProfile(
                "TBOX_DEVICE_ECDH_ES_P256_V1",
                "TBOX_DEVICE_ECDH_ES_P256_V1",
                CertificateProfile.SubjectType.DEVICE_IDENTITY,
                "EC",
                "DIGITAL_SIGNATURE,KEY_AGREEMENT");
    }
}
