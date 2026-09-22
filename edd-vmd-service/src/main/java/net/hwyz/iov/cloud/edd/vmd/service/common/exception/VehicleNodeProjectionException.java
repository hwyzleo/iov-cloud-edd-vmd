package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * 车载节点投影契约错误异常（CR-049）
 * <p>
 * MDM 快照 / 事件 payload 缺失必需字段（code/externalRefId/version）时抛出。
 * 不新增业务错误码，由消息消费者捕获并计入同步失败指标、走现有重试/DLQ，不写半条投影。
 * </p>
 *
 * @author hwyz_leo
 * @since 2026-09-22
 */
public class VehicleNodeProjectionException extends RuntimeException {

    public VehicleNodeProjectionException(String message) {
        super(message);
    }

    public VehicleNodeProjectionException(String message, Throwable cause) {
        super(message, cause);
    }

}
