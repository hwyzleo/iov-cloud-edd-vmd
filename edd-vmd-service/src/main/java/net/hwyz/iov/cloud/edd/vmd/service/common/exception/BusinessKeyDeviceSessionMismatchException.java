package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * 设备会话身份不一致异常（CR-055 业务密钥域）
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
public class BusinessKeyDeviceSessionMismatchException extends VmdBaseException {

    public BusinessKeyDeviceSessionMismatchException(String detail) {
        super(VmdErrorCode.BUSINESS_KEY_DEVICE_SESSION_MISMATCH, detail);
    }

    public BusinessKeyDeviceSessionMismatchException() {
        super(VmdErrorCode.BUSINESS_KEY_DEVICE_SESSION_MISMATCH);
    }
}
