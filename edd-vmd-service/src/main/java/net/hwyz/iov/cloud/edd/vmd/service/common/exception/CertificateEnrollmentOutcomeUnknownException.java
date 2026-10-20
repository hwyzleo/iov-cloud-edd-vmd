package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 证书签发结果未知异常
 * <p>
 * VMD-DSN-CR-063：enrollment 调用已发出但 timeout/read failure 后结果未知，禁止自动重签，
 * 只允许按原 framework requestId/status/result store 对账。
 *
 * @author hwyz_leo
 * @since 2026-10-20
 */
@Slf4j
public class CertificateEnrollmentOutcomeUnknownException extends VmdBaseException {

    public CertificateEnrollmentOutcomeUnknownException(String detail) {
        super(VmdErrorCode.CERTIFICATE_ENROLLMENT_OUTCOME_UNKNOWN, detail);
        log.warn("证书签发结果未知：{}", detail);
    }

}
