package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * KMS/HSM服务不可用异常
 * <p>
 * CR-037 起触发场景扩为进程内 framework-security 依赖/KMS 通道不可用
 * （映射 framework CryptoDependencyUnavailableException）
 *
 * @author hwyz_leo
 * @since 2026-07-08
 */
public class KmsHsmUnavailableException extends VmdBaseException {

    public KmsHsmUnavailableException() {
        super(VmdErrorCode.KMS_HSM_UNAVAILABLE);
    }

    public KmsHsmUnavailableException(String detail) {
        super(VmdErrorCode.KMS_HSM_UNAVAILABLE, detail);
    }
}
