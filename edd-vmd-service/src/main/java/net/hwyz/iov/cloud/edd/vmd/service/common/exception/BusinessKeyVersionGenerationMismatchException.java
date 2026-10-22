package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * 业务密钥版本代际冲突异常（VMD-DSN-CR-066 §6.1/§8）。
 * <p>
 * 设备声明的 businessKeyVersion 与目录实际密码学代际不一致（同代际但版本不同），
 * 属版本代际冲突，<strong>不可重试</strong>（DO_NOT_RETRY）；外部车端声明不得强制提升业务版本。
 *
 * @author hwyz_leo
 * @since 2026-10-22
 */
public class BusinessKeyVersionGenerationMismatchException extends VmdBaseException {

    public BusinessKeyVersionGenerationMismatchException(String detail) {
        super(VmdErrorCode.KEYPROV_VERSION_GENERATION_MISMATCH, detail);
    }

    public BusinessKeyVersionGenerationMismatchException() {
        super(VmdErrorCode.KEYPROV_VERSION_GENERATION_MISMATCH);
    }
}
