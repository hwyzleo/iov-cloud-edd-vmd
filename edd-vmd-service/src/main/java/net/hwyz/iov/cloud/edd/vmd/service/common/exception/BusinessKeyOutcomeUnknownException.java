package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * 密钥操作结果未知需对账异常（CR-055 业务密钥域）
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
public class BusinessKeyOutcomeUnknownException extends VmdBaseException {

    public BusinessKeyOutcomeUnknownException(String detail) {
        super(VmdErrorCode.BUSINESS_KEY_OUTCOME_UNKNOWN, detail);
    }

    public BusinessKeyOutcomeUnknownException() {
        super(VmdErrorCode.BUSINESS_KEY_OUTCOME_UNKNOWN);
    }
}
