package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * 业务密钥目录多ACTIVE异常（CR-055 业务密钥域）
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
public class BusinessKeyMultipleActiveException extends VmdBaseException {

    public BusinessKeyMultipleActiveException(String detail) {
        super(VmdErrorCode.BUSINESS_KEY_MULTIPLE_ACTIVE, detail);
    }

    public BusinessKeyMultipleActiveException() {
        super(VmdErrorCode.BUSINESS_KEY_MULTIPLE_ACTIVE);
    }
}
