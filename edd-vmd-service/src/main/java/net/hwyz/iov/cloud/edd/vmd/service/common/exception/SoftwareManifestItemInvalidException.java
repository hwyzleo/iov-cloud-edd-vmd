package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 软件实装清单单条 item 非法异常
 * <p>
 * VMD-DSN-CR-045/046: 软件节点 / target / 版本缺失或格式错
 *
 * @author hwyz_leo
 * @since 2026-09-08
 */
@Slf4j
public class SoftwareManifestItemInvalidException extends VmdBaseException {

    public SoftwareManifestItemInvalidException(String reason) {
        super(VmdErrorCode.SOFTWARE_MANIFEST_ITEM_INVALID, reason);
        log.warn("软件实装清单条目非法: {}", reason);
    }
}
