package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * 配置投影契约错误异常（CR-047）
 * <p>
 * MDM 快照 / 事件 payload 缺失必需字段（code/name/variantCode/externalRefId/version）时抛出。
 * 不新增业务错误码（§5.3 806xxx 无对应码），由消息消费者捕获并计入同步失败指标、走现有重试/DLQ，
 * 不写半条投影（RD-047-3）。
 * </p>
 *
 * @author hwyz_leo
 */
public class ConfigurationProjectionException extends RuntimeException {

    public ConfigurationProjectionException(String message) {
        super(message);
    }

    public ConfigurationProjectionException(String message, Throwable cause) {
        super(message, cause);
    }

}
