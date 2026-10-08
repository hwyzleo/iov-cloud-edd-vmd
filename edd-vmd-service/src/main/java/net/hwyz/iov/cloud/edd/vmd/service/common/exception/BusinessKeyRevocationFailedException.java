package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * 业务密钥吊销失败异常（CR-055 业务密钥域）
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
public class BusinessKeyRevocationFailedException extends VmdBaseException {

    public BusinessKeyRevocationFailedException(String detail) {
        super(VmdErrorCode.BUSINESS_KEY_REVOCATION_FAILED, detail);
    }

    public BusinessKeyRevocationFailedException() {
        super(VmdErrorCode.BUSINESS_KEY_REVOCATION_FAILED);
    }
}
