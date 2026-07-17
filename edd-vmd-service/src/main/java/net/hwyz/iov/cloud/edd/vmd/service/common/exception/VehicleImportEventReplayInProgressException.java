package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 车辆导入事件补发进行中异常
 * <p>
 * VMD-DSN-CR-039: 车辆导入成功事件人工补发
 *
 * @author hwyz_leo
 * @since 2026-07-17
 */
@Slf4j
public class VehicleImportEventReplayInProgressException extends VmdBaseException {

    public VehicleImportEventReplayInProgressException(Long vehImportDataId) {
        super(VmdErrorCode.VEHICLE_IMPORT_EVENT_REPLAY_IN_PROGRESS);
        log.warn("车辆导入记录[{}]正在补发消息，请勿重复操作", vehImportDataId);
    }
}
