package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 缺少软件实装来源版本或时刻异常
 * <p>
 * VMD-DSN-CR-045/046: 缺 sourceVersion / occurredAt，无法做版本时序判定
 *
 * @author hwyz_leo
 * @since 2026-09-08
 */
@Slf4j
public class SoftwareSourceVersionMissingException extends VmdBaseException {

    public SoftwareSourceVersionMissingException(String reason) {
        super(VmdErrorCode.SOFTWARE_SOURCE_VERSION_MISSING, reason);
        log.warn("缺少软件实装来源版本或时刻: {}", reason);
    }
}
