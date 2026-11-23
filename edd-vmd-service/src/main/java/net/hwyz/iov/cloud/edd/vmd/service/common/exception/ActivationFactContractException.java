package net.hwyz.iov.cloud.edd.vmd.service.common.exception;

/**
 * 激活事实契约错误（VMD-DSN-CR-077 / D-VMD-077-06）
 * <p>
 * 可隔离但不可重试的错误：缺必填、类型非法、{@code schemaVersion != 1}、{@code vin} 超长、
 * {@code occurredAt} 不可解析等。消费者捕获后立即落台账 {@code QUARANTINED}，
 * <strong>不抛异常、不重试、不阻塞分区</strong>。
 * <p>
 * 本异常不占用对外错误码（对外仅登记 806137 查询不可用，R-18 #4）。
 *
 * @author hwyz_leo
 */
public class ActivationFactContractException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ActivationFactContractException(String message) {
        super(message);
    }

    public ActivationFactContractException(String message, Throwable cause) {
        super(message, cause);
    }

}
