package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 零件不存在异常
 *
 * @author hwyz_leo
 */
@Slf4j
public class PartNotExistException extends VmdBaseException {

    public PartNotExistException(String pn, String sn) {
        super(ERROR_CODE_PART_NOT_EXIST);
        log.warn("零件[{}:{}]不存在", pn, sn);
    }

}
