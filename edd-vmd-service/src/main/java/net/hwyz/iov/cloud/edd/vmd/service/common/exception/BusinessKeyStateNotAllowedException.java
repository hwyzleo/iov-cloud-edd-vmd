package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * 业务密钥状态不允许异常（CR-055 业务密钥域）
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
public class BusinessKeyStateNotAllowedException extends VmdBaseException {

    public BusinessKeyStateNotAllowedException(String detail) {
        super(VmdErrorCode.BUSINESS_KEY_STATE_NOT_ALLOWED, detail);
    }

    public BusinessKeyStateNotAllowedException() {
        super(VmdErrorCode.BUSINESS_KEY_STATE_NOT_ALLOWED);
    }
}
