package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 车辆导入数据异常
 *
 * @author hwyz_leo
 */
@Slf4j
public class VehicleImportDataException extends VmdBaseException {

    public VehicleImportDataException(String batchNum, String reason) {
        super(ERROR_CODE_VEHICLE_IMPORT_DATA_EXCEPTION);
        log.error("车辆导入数据[{}]异常[{}]", batchNum, reason);
    }

}
