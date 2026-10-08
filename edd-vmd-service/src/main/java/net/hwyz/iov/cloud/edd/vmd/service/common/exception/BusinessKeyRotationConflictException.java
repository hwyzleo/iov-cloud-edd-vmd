package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * 业务密钥轮换并发冲突异常（CR-055 业务密钥域）
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
public class BusinessKeyRotationConflictException extends VmdBaseException {

    public BusinessKeyRotationConflictException(String detail) {
        super(VmdErrorCode.BUSINESS_KEY_ROTATION_CONFLICT, detail);
    }

    public BusinessKeyRotationConflictException() {
        super(VmdErrorCode.BUSINESS_KEY_ROTATION_CONFLICT);
    }
}
