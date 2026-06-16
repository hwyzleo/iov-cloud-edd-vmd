package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 零件不允许绑定异常
 *
 * @author hwyz_leo
 */
@Slf4j
public class PartNotAllowBindException extends VmdBaseException {

    public PartNotAllowBindException(String code, String sn, Integer partState) {
        super(VmdErrorCode.PART_NOT_ALLOW_BIND);
        log.warn("零件[{}:{}]状态[{}]不允许绑定", code, sn, partState);
    }

}
