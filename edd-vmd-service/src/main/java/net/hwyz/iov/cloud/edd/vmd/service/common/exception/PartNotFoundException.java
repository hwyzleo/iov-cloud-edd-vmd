package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 零件编码在MDM主数据中不存在异常
 *
 * @author hwyz_leo
 */
@Slf4j
public class PartNotFoundException extends VmdBaseException {

    public PartNotFoundException(String partCode) {
        super(VmdErrorCode.PART_NOT_FOUND_IN_MDM);
        log.warn("零件编码[{}]在MDM主数据中不存在", partCode);
    }

}
