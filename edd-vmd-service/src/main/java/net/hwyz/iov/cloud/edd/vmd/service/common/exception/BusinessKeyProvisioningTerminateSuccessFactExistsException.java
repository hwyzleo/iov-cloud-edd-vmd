package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * 业务密钥开通批次存在成功事实，禁止终止异常（VMD-DSN-CR-065）
 *
 * @author hwyz_leo
 * @since 2026-10-22
 */
public class BusinessKeyProvisioningTerminateSuccessFactExistsException extends VmdBaseException {

    public BusinessKeyProvisioningTerminateSuccessFactExistsException(String detail) {
        super(VmdErrorCode.BUSINESS_KEY_PROVISIONING_TERMINATE_SUCCESS_FACT_EXISTS, detail);
    }
}
