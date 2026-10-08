package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * 有效设备证书不存在异常（CR-055 业务密钥域）
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
public class BusinessKeyDeviceCertNotFoundException extends VmdBaseException {

    public BusinessKeyDeviceCertNotFoundException(String detail) {
        super(VmdErrorCode.BUSINESS_KEY_DEVICE_CERT_NOT_FOUND, detail);
    }

    public BusinessKeyDeviceCertNotFoundException() {
        super(VmdErrorCode.BUSINESS_KEY_DEVICE_CERT_NOT_FOUND);
    }
}
