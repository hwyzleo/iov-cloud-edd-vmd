package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 零件不允许绑定异常
 *
 * @author hwyz_leo
 */
@Slf4j
public class PartNotAllowBindException extends VmdBaseException {

    public PartNotAllowBindException(String pn, String sn, Integer partState) {
        super(ERROR_CODE_PART_NOT_ALLOW_BIND);
        log.warn("零件[{}:{}]状态[{}]不允许绑定", pn, sn, partState);
    }

}
