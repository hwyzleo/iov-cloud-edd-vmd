package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * 版本投影契约错误异常（CR-048）
 * <p>
 * MDM 快照 / 事件 payload 缺失必需字段（code/name/modelCode/externalRefId/version）时抛出。
 * 不新增业务错误码，由消息消费者捕获并计入同步失败指标、走现有重试/DLQ，不写半条投影（RD-048-3）。
 * </p>
 *
 * @author hwyz_leo
 */
public class VariantProjectionException extends RuntimeException {

    public VariantProjectionException(String message) {
        super(message);
    }

    public VariantProjectionException(String message, Throwable cause) {
        super(message, cause);
    }

}
