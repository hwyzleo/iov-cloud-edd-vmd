package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * 业务密钥不存在异常（CR-055 业务密钥域）
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
public class BusinessKeyNotExistException extends VmdBaseException {

    public BusinessKeyNotExistException(String detail) {
        super(VmdErrorCode.BUSINESS_KEY_NOT_EXIST, detail);
    }

    public BusinessKeyNotExistException() {
        super(VmdErrorCode.BUSINESS_KEY_NOT_EXIST);
    }
}
