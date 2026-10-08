package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * 业务密钥设备封装失败异常（CR-055 业务密钥域）
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
public class BusinessKeyWrapFailedException extends VmdBaseException {

    public BusinessKeyWrapFailedException(String detail) {
        super(VmdErrorCode.BUSINESS_KEY_WRAP_FAILED, detail);
    }

    public BusinessKeyWrapFailedException() {
        super(VmdErrorCode.BUSINESS_KEY_WRAP_FAILED);
    }
}
