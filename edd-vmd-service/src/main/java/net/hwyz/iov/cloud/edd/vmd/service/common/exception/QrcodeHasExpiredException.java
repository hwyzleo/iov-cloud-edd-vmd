package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.edd.vmd.api.vo.enums.QrcodeType;

/**
 * 车辆对应类型二维码已过期异常
 *
 * @author hwyz_leo
 */
@Slf4j
public class QrcodeHasExpiredException extends VmdBaseException {

    public QrcodeHasExpiredException(String vin, QrcodeType type) {
        super(ERROR_CODE_QRCODE_HAS_EXPIRED);
        log.warn("车辆[{}]对应类型[{}]二维码已过期", vin, type);
    }

}
