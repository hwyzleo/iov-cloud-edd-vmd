package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * 零件实例入站校验失败异常
 *
 * @author hwyz_leo
 */
public class PartInboundValidateFailedException extends VmdBaseException {

    public PartInboundValidateFailedException(String message) {
        super(VmdErrorCode.PART_INBOUND_VALIDATE_FAILED, message);
    }
}
