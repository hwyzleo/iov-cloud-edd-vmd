package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

/**
 * MDM 消费 Topic 预检内部异常（携带错误分类，不记录凭证）。
 *
 * @author hwyz_leo
 */
public class MdmPreflightException extends RuntimeException {

    private final MdmPreflightClassification classification;

    public MdmPreflightException(MdmPreflightClassification classification, String message) {
        super(message);
        this.classification = classification;
    }

    public MdmPreflightClassification classification() {
        return classification;
    }
}
