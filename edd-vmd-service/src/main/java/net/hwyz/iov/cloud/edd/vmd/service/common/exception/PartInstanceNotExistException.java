package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 物理零件实例不存在异常
 *
 * @author hwyz_leo
 */
@Slf4j
public class PartInstanceNotExistException extends VmdBaseException {

    public PartInstanceNotExistException(String partCode, String sn) {
        super(VmdErrorCode.PART_INSTANCE_NOT_EXIST);
        log.warn("物理零件实例[{}:{}]不存在", partCode, sn);
    }

}
