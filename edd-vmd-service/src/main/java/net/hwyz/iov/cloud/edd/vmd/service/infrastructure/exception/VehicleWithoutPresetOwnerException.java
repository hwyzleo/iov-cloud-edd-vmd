package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 车辆没有设置预设车主异常
 *
 * @author hwyz_leo
 */
@Slf4j
public class VehicleWithoutPresetOwnerException extends VmdBaseException {

    public VehicleWithoutPresetOwnerException(String vin) {
        super(ERROR_CODE_VEHICLE_WITHOUT_PRESET_OWNER);
        log.warn("车辆[{}]没有设置预设车主", vin);
    }

}
