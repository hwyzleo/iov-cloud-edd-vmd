package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * 业务密钥开通批次不存在异常（CR-058）
 *
 * @author hwyz_leo
 * @since 2026-10-12
 */
public class BusinessKeyProvisioningNotExistException extends VmdBaseException {

    public BusinessKeyProvisioningNotExistException(String detail) {
        super(VmdErrorCode.BUSINESS_KEY_PROVISIONING_NOT_FOUND, detail);
    }
}
