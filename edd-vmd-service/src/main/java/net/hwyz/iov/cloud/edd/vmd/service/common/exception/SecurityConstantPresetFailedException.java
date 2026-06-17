package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * 安全常量预置失败异常
 *
 * @author hwyz_leo
 * @since 2026-06-17
 */
public class SecurityConstantPresetFailedException extends VmdBaseException {

    public SecurityConstantPresetFailedException() {
        super(VmdErrorCode.SECURITY_CONSTANT_PRESET_FAILED);
    }

    public SecurityConstantPresetFailedException(String detail) {
        super(VmdErrorCode.SECURITY_CONSTANT_PRESET_FAILED, detail);
    }
}
