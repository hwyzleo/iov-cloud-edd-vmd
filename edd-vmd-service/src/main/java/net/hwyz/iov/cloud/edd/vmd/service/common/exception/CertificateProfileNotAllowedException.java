package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 证书Profile不允许异常
 * <p>
 * CR-054：证书 Profile 不在白名单时拒绝签发。
 *
 * @author hwyz_leo
 * @since 2026-09-29
 */
@Slf4j
public class CertificateProfileNotAllowedException extends VmdBaseException {

    public CertificateProfileNotAllowedException(String certificateProfile) {
        super(VmdErrorCode.CERTIFICATE_PROFILE_NOT_ALLOWED);
        log.warn("证书Profile不允许：{}", certificateProfile);
    }

}
