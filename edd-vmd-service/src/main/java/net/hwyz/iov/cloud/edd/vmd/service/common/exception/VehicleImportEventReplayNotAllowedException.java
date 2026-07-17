package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 车辆导入事件补发不允许异常
 * <p>
 * VMD-DSN-CR-039: 车辆导入成功事件人工补发
 *
 * @author hwyz_leo
 * @since 2026-07-17
 */
@Slf4j
public class VehicleImportEventReplayNotAllowedException extends VmdBaseException {

    public VehicleImportEventReplayNotAllowedException(String reason) {
        super(VmdErrorCode.VEHICLE_IMPORT_EVENT_REPLAY_NOT_ALLOWED, reason);
        log.warn("车辆导入记录不允许补发消息: {}", reason);
    }
}
