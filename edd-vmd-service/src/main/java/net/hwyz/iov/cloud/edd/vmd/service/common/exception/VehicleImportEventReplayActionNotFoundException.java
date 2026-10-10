package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 车辆导入补发动作未登记异常
 * <p>
 * VMD-DSN-CR-057: 车辆导入补发动作注册表
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Slf4j
public class VehicleImportEventReplayActionNotFoundException extends VmdBaseException {

    public VehicleImportEventReplayActionNotFoundException(String actionType) {
        super(VmdErrorCode.VEHICLE_IMPORT_REPLAY_ACTION_NOT_REGISTERED);
        log.warn("车辆导入补发动作[{}]未登记，不允许执行", actionType);
    }
}
