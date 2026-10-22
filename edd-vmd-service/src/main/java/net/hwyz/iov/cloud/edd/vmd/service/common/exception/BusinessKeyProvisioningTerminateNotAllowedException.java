package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * 业务密钥开通批次状态不允许运维终止或未超滞留阈值异常（VMD-DSN-CR-065）
 *
 * @author hwyz_leo
 * @since 2026-10-22
 */
public class BusinessKeyProvisioningTerminateNotAllowedException extends VmdBaseException {

    public BusinessKeyProvisioningTerminateNotAllowedException(String detail) {
        super(VmdErrorCode.BUSINESS_KEY_PROVISIONING_TERMINATE_NOT_ALLOWED, detail);
    }
}
