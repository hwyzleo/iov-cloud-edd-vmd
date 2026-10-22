package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * 业务密钥开通批次存在结果未知的operation，需先对账异常（VMD-DSN-CR-065）
 *
 * @author hwyz_leo
 * @since 2026-10-22
 */
public class BusinessKeyProvisioningTerminateReconcileFirstException extends VmdBaseException {

    public BusinessKeyProvisioningTerminateReconcileFirstException(String detail) {
        super(VmdErrorCode.BUSINESS_KEY_PROVISIONING_TERMINATE_RECONCILE_FIRST, detail);
    }
}
