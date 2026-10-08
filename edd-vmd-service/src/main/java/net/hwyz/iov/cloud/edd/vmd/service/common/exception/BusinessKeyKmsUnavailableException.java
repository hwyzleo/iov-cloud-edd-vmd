package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * KMS/HSM或framework安全服务不可用异常（CR-055 业务密钥域）
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
public class BusinessKeyKmsUnavailableException extends VmdBaseException {

    public BusinessKeyKmsUnavailableException(String detail) {
        super(VmdErrorCode.BUSINESS_KEY_KMS_UNAVAILABLE, detail);
    }

    public BusinessKeyKmsUnavailableException() {
        super(VmdErrorCode.BUSINESS_KEY_KMS_UNAVAILABLE);
    }
}
