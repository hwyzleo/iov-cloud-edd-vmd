package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * KMS/HSM不可用异常
 *
 * @author hwyz_leo
 * @since 2026-06-17
 */
public class KmsHsmUnavailableException extends VmdBaseException {

    public KmsHsmUnavailableException() {
        super(VmdErrorCode.KMS_HSM_UNAVAILABLE);
    }

    public KmsHsmUnavailableException(String detail) {
        super(VmdErrorCode.KMS_HSM_UNAVAILABLE, detail);
    }
}
