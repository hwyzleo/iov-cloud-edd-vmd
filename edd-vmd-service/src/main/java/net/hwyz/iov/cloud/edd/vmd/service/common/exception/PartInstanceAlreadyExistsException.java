package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 物理零件实例已存在异常
 *
 * @author hwyz_leo
 */
@Slf4j
public class PartInstanceAlreadyExistsException extends VmdBaseException {

    public PartInstanceAlreadyExistsException(String partCode, String sn) {
        super(VmdErrorCode.PART_INSTANCE_ALREADY_EXISTS);
        log.warn("物理零件实例[{}:{}]已存在", partCode, sn);
    }

}
