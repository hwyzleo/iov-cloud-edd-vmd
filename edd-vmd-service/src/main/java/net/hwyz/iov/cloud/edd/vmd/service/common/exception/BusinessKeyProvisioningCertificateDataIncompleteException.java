package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * 证书关联数据不完整异常（VMD-DSN-CR-065 §6.2 寻址前置校验）。
 * <p>
 * 证书申请记录存在但缺少 deviceSn 或证书序列号，无法解析到开通批次维度时抛出。
 *
 * @author hwyz_leo
 * @since 2026-10-22
 */
public class BusinessKeyProvisioningCertificateDataIncompleteException extends VmdBaseException {

    public BusinessKeyProvisioningCertificateDataIncompleteException(String detail) {
        super(VmdErrorCode.BUSINESS_KEY_PROVISIONING_CERTIFICATE_DATA_INCOMPLETE, detail);
    }
}
