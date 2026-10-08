package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * 业务密钥幂等键冲突异常（CR-055 业务密钥域）
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
public class BusinessKeyIdempotencyConflictException extends VmdBaseException {

    public BusinessKeyIdempotencyConflictException(String detail) {
        super(VmdErrorCode.BUSINESS_KEY_IDEMPOTENCY_CONFLICT, detail);
    }

    public BusinessKeyIdempotencyConflictException() {
        super(VmdErrorCode.BUSINESS_KEY_IDEMPOTENCY_CONFLICT);
    }
}
