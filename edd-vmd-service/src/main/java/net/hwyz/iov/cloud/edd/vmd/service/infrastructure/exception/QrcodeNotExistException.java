package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 车辆对应类型二维码不存在异常
 *
 * @author hwyz_leo
 */
@Slf4j
public class QrcodeNotExistException extends VmdBaseException {

    public QrcodeNotExistException(String qrcode) {
        super(ERROR_CODE_QRCODE_NOT_EXIST);
        log.warn("车辆二维码[{}]不存在", qrcode);
    }

}
