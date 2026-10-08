package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * 业务域或用途未授权异常（CR-055 业务密钥域）
 *
 * @author hwyz_leo
 * @since 2026-10-08
 */
public class BusinessKeyDomainNotAuthorizedException extends VmdBaseException {

    public BusinessKeyDomainNotAuthorizedException(String detail) {
        super(VmdErrorCode.BUSINESS_KEY_DOMAIN_NOT_AUTHORIZED, detail);
    }

    public BusinessKeyDomainNotAuthorizedException() {
        super(VmdErrorCode.BUSINESS_KEY_DOMAIN_NOT_AUTHORIZED);
    }
}
