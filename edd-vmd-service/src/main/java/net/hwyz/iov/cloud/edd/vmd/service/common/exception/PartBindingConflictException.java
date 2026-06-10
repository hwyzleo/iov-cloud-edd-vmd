package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 零件绑定冲突异常
 *
 * @author hwyz_leo
 */
@Slf4j
public class PartBindingConflictException extends VmdBaseException {

    public PartBindingConflictException(String message) {
        super(VmdErrorCode.PART_BINDING_CONFLICT);
        log.warn("零件绑定冲突: {}", message);
    }

}
