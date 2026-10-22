package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * 业务密钥开通批次页面视图已过期，请刷新后重试异常（VMD-DSN-CR-065）
 *
 * @author hwyz_leo
 * @since 2026-10-22
 */
public class BusinessKeyProvisioningTerminateStaleViewException extends VmdBaseException {

    public BusinessKeyProvisioningTerminateStaleViewException(String detail) {
        super(VmdErrorCode.BUSINESS_KEY_PROVISIONING_TERMINATE_STALE_VIEW, detail);
    }
}
