package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 车辆已激活异常
 *
 * @author hwyz_leo
 */
@Slf4j
public class VehicleHasActivatedException extends VmdBaseException {

    public VehicleHasActivatedException(String vin) {
        super(ERROR_CODE_VEHICLE_HAS_ACTIVATED);
        log.warn("车辆[{}]已激活", vin);
    }

}
