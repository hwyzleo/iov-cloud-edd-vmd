package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * OTA根未预置异常
 * <p>
 * OTA根下发时该 VIN 的 OTA 根未预置
 * （veh_security_constant 无 constant_type=OTA 的 PRESET 记录）
 *
 * @author hwyz_leo
 * @since 2026-07-09
 */
@Slf4j
public class OtaRootNotPresetException extends VmdBaseException {

    public OtaRootNotPresetException(String vin) {
        super(VmdErrorCode.OTA_ROOT_NOT_PRESET);
        log.warn("车辆[{}]OTA根未预置", vin);
    }
}
