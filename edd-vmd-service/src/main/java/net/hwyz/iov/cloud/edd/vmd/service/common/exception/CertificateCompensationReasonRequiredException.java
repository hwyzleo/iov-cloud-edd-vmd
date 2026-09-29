package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 证书补偿缺少原因/工单异常
 * <p>
 * CR-053：人工补偿申请或安装结果补录缺少必要原因 / 工单信息。
 *
 * @author hwyz_leo
 */
@Slf4j
public class CertificateCompensationReasonRequiredException extends VmdBaseException {

    public CertificateCompensationReasonRequiredException() {
        super(VmdErrorCode.CERTIFICATE_COMPENSATION_REASON_REQUIRED);
        log.warn("证书补偿操作缺少人工原因/必要工单信息");
    }

}
