package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 车辆已绑定订单异常
 *
 * @author hwyz_leo
 */
@Slf4j
public class VehicleHasBindOrderException extends VmdBaseException {

    public VehicleHasBindOrderException(String vin, String orderNum) {
        super(ERROR_CODE_VEHICLE_HAS_BIND_ORDER);
        log.warn("车辆[{}]已绑定订单[{}]", vin, orderNum);
    }

}
