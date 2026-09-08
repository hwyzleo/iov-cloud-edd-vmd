package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 软件实装清单整体校验失败异常
 * <p>
 * VMD-DSN-CR-045/046: 结构错误 / 必填缺失（事件级契约错误）
 *
 * @author hwyz_leo
 * @since 2026-09-08
 */
@Slf4j
public class SoftwareManifestInvalidException extends VmdBaseException {

    public SoftwareManifestInvalidException(String reason) {
        super(VmdErrorCode.SOFTWARE_MANIFEST_INVALID, reason);
        log.warn("软件实装清单校验失败: {}", reason);
    }
}
