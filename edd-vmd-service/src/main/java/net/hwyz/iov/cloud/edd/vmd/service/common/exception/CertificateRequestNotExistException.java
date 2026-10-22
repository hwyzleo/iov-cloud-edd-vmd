package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * 证书申请不存在异常（CR-053 证书域，VMD-DSN-CR-065 §6.4 寻址错误语义沿用）。
 *
 * @author hwyz_leo
 * @since 2026-10-22
 */
public class CertificateRequestNotExistException extends VmdBaseException {

    public CertificateRequestNotExistException(String detail) {
        super(VmdErrorCode.CERTIFICATE_REQUEST_NOT_EXIST, detail);
    }
}
