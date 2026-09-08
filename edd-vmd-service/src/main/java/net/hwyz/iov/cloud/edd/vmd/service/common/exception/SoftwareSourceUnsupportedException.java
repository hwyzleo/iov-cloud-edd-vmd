package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 不支持的软件实装来源异常
 * <p>
 * VMD-DSN-CR-045/046: source 不在枚举内或不被允许写入
 *
 * @author hwyz_leo
 * @since 2026-09-08
 */
@Slf4j
public class SoftwareSourceUnsupportedException extends VmdBaseException {

    public SoftwareSourceUnsupportedException(String source) {
        super(VmdErrorCode.SOFTWARE_SOURCE_UNSUPPORTED, "不支持的来源: " + source);
        log.warn("不支持的软件实装来源: {}", source);
    }
}
