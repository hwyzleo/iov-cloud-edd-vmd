package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 证书申请当前状态不允许补偿异常
 * <p>
 * CR-053：reconcile 仅允许 REQUESTED / ISSUING / PENDING_RECONCILE；
 * 终态 ACTIVE / SUPERSEDED / REVOKED / EXPIRED 禁止再次补偿签发。
 *
 * @author hwyz_leo
 */
@Slf4j
public class CertificateCompensationNotAllowedException extends VmdBaseException {

    public CertificateCompensationNotAllowedException(String requestId, String status) {
        super(VmdErrorCode.CERTIFICATE_COMPENSATION_NOT_ALLOWED);
        log.warn("证书申请[{}]当前状态[{}]不允许补偿", requestId, status);
    }

}
