package net.hwyz.iov.cloud.edd.vmd.service.application.service.certificateprofile;

import net.hwyz.iov.cloud.framework.security.crypto.model.CertificateProfile;
import net.hwyz.iov.cloud.framework.security.crypto.model.DeviceCertificateProfile;

/**
 * 注册的证书 Profile（VMD-DSN-CR-063 §4.1）。
 * <p>
 * 固定映射一个 VMD 逻辑 Profile → FW-SEC 校验 Profile（CR-017 validator 用
 * {@link DeviceCertificateProfile}）→ framework 签发 Profile（{@link CertificateProfile} record，
 * CertApplyRequest 用）→ 设备类别 → keyprov 资格。
 *
 * @param vmdProfile        VMD 逻辑 Profile
 * @param frameworkProfile  FW-SEC CR-017 校验用 Profile（DeviceCertificateProfile 枚举）
 * @param applyProfile      framework 签发申请用 Profile（CertificateProfile record）
 * @param deviceCategory    该 Profile 允许的设备类别
 * @param keyprovEligible   是否可作 CR-062 ECDH-ES Keyprov 收件方证书（true 才允许进入新 Request 门禁）
 * @param enabled           逻辑开关（enabled=false 时拒绝签发并关闭对应 Keyprov 能力）
 * @author hwyz_leo
 * @since 2026-10-20
 */
public record RegisteredCertificateProfile(
        VmdCertificateProfile vmdProfile,
        DeviceCertificateProfile frameworkProfile,
        CertificateProfile applyProfile,
        String deviceCategory,
        boolean keyprovEligible,
        boolean enabled
) {
}
