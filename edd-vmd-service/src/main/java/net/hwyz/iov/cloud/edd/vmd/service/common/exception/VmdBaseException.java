package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.framework.common.exception.ErrorCode;

/**
 * 车辆主数据服务基础异常
 *
 * @author hwyz_leo
 */
public class VmdBaseException extends BusinessException {

    public VmdBaseException(ErrorCode errorCode) {
        super(errorCode);
    }

    public VmdBaseException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }

}
