package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 零件在MDM主数据中非ACTIVE状态异常
 *
 * @author hwyz_leo
 */
@Slf4j
public class PartNotActiveException extends VmdBaseException {

    public PartNotActiveException(String partCode) {
        super(VmdErrorCode.PART_NOT_ACTIVE_IN_MDM);
        log.warn("零件[{}]在MDM主数据中非ACTIVE状态", partCode);
    }

}
