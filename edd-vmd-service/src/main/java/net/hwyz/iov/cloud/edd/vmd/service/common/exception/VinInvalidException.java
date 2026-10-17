package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * VIN 规范化校验失败异常（VMD-DSN-CR-061 RD-061-2）。
 * <p>
 * 规范化规则：{@code trim → 大写(Locale.ROOT) → 长度恰 17 → 字符集 [A-HJ-NPR-Z0-9]}（排除 I/O/Q）。
 * 不合法时 fail-closed，且**异常正文不得包含 VIN 原文**（避免敏感标识进入日志/响应）。
 *
 * @author hwyz_leo
 * @since 2026-10-17
 */
public class VinInvalidException extends VmdBaseException {

    public VinInvalidException(String detail) {
        super(VmdErrorCode.KEYPROV_VIN_BINDING_INVALID, detail);
    }

    public VinInvalidException() {
        super(VmdErrorCode.KEYPROV_VIN_BINDING_INVALID);
    }
}