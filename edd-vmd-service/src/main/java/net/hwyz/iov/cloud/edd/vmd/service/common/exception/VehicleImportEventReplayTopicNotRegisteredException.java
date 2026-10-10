package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 车辆导入补发目标 Topic 未登记异常
 * <p>
 * VMD-DSN-CR-057: 车辆导入补发动作注册表
 *
 * @author hwyz_leo
 * @since 2026-10-10
 */
@Slf4j
public class VehicleImportEventReplayTopicNotRegisteredException extends VmdBaseException {

    public VehicleImportEventReplayTopicNotRegisteredException(String eventType) {
        super(VmdErrorCode.VEHICLE_IMPORT_REPLAY_TOPIC_NOT_REGISTERED);
        log.warn("车辆导入补发目标Topic未登记，不允许发布: eventType={}", eventType);
    }
}
