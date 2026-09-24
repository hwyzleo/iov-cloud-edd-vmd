package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

/**
 * 单投影预检结果（VMD-DSN-CR-052 §5）
 *
 * @param spec           预检规格
 * @param topic          实际 Topic 名称
 * @param ok             是否通过
 * @param classification 错误分类（ok=true 时为 null）
 * @param message        失败原因（不记录凭证）
 * @author hwyz_leo
 */
public record MdmPreflightResult(
        MdmConsumerTopicSpec spec,
        String topic,
        boolean ok,
        MdmPreflightClassification classification,
        String message) {

    /**
     * 预检通过。
     */
    public static MdmPreflightResult ok(MdmConsumerTopicSpec spec) {
        return new MdmPreflightResult(spec, spec.topicName(), true, null, null);
    }

    /**
     * 预检失败。
     */
    public static MdmPreflightResult failed(MdmConsumerTopicSpec spec,
                                            MdmPreflightClassification classification,
                                            String message) {
        return new MdmPreflightResult(spec, spec.topicName(), false, classification, message);
    }

    /**
     * 指标 result 标签值（present / missing / unauthorized / timeout / error）。
     */
    public String resultTag() {
        return ok ? "present" : classification.tag();
    }
}
