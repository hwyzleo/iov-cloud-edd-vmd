package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 防盗根未预置异常
 * <p>
 * 防盗根下发时该 VIN 的 IMMO 防盗根未预置
 * （veh_security_constant 无 constant_type=IMMO 的 PRESET 记录）
 *
 * @author hwyz_leo
 * @since 2026-07-08
 */
@Slf4j
public class ImmoRootNotPresetException extends VmdBaseException {

    public ImmoRootNotPresetException(String vin) {
        super(VmdErrorCode.IMMO_ROOT_NOT_PRESET);
        log.warn("车辆[{}]防盗根未预置", vin);
    }
}
