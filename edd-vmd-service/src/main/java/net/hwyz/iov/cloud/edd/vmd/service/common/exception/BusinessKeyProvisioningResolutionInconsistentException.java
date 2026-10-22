package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * 证书维度寻址数据一致性异常（VMD-DSN-CR-065 §6.2 算法第 3 步）。
 * <p>
 * 同设备+业务域存在多于一条未终结开通批次时 fail-closed，不得任选一条返回。
 *
 * @author hwyz_leo
 * @since 2026-10-22
 */
public class BusinessKeyProvisioningResolutionInconsistentException extends VmdBaseException {

    public BusinessKeyProvisioningResolutionInconsistentException(String detail) {
        super(VmdErrorCode.BUSINESS_KEY_PROVISIONING_RESOLUTION_INCONSISTENT, detail);
    }
}
