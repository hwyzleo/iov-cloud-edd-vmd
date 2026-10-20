package net.hwyz.iov.cloud.edd.vmd.service.application.service.certificateprofile;

import java.util.Set;

/**
 * 证书 Profile 注册表（VMD-DSN-CR-063 §4.1）。
 * <p>
 * 外部字符串经严格枚举解析；未知、停用或设备类别不匹配均拒绝。不得把外部字符串
 * 直接当作物理 Provisioner 名称。
 *
 * @author hwyz_leo
 * @since 2026-10-20
 */
public interface CertificateProfileRegistry {

    /**
     * 按逻辑 Profile 取注册项；未注册/停用抛 {@code CertificateProfileNotAllowedException}。
     *
     * @param profile VMD 逻辑 Profile
     * @return 注册项（enabled=true）
     */
    RegisteredCertificateProfile requireEnabled(VmdCertificateProfile profile);

    /**
     * 按外部字符串严格解析后取注册项；未知/停用抛 {@code CertificateProfileNotAllowedException}。
     *
     * @param externalProfile 外部 Profile 字符串
     * @return 注册项（enabled=true）
     */
    RegisteredCertificateProfile requireEnabled(String externalProfile);

    /**
     * 判断逻辑 Profile 是否已注册且启用
     */
    boolean isEnabled(VmdCertificateProfile profile);

    /**
     * 已注册的逻辑 Profile 集合（含禁用项，供 readiness/审计）
     */
    Set<VmdCertificateProfile> registeredProfiles();
}
