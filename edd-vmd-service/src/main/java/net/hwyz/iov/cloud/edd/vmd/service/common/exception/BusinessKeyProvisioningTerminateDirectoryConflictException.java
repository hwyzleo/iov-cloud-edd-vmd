package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * 业务密钥开通批次目录归属冲突，禁止终止异常（VMD-DSN-CR-065）
 *
 * @author hwyz_leo
 * @since 2026-10-22
 */
public class BusinessKeyProvisioningTerminateDirectoryConflictException extends VmdBaseException {

    public BusinessKeyProvisioningTerminateDirectoryConflictException(String detail) {
        super(VmdErrorCode.BUSINESS_KEY_PROVISIONING_TERMINATE_DIRECTORY_CONFLICT, detail);
    }
}
