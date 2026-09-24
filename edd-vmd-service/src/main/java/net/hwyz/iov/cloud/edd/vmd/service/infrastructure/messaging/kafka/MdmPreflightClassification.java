package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

/**
 * MDM 消费 Topic 预检错误分类（VMD-DSN-CR-052 §5）
 * <p>
 * 预检失败按分类输出逻辑名、实际 Topic、投影类型、集群、错误分类与处置建议，
 * 不记录凭证（ACL 拒绝只输出分类，不输出权限明细）。
 *
 * @author hwyz_leo
 */
public enum MdmPreflightClassification {

    /**
     * Topic 不存在（上游未创建，VMD 不创建）。
     */
    MISSING("missing", "Topic 缺失（归 EDD-MDM 创建，VMD 只预检不创建）"),

    /**
     * Describe / Read / ConsumerGroup 权限不足。
     */
    UNAUTHORIZED("unauthorized", "ACL 不足：需要 Describe/Read（Topic）与 Group 权限（ConsumerGroup）"),

    /**
     * Broker 超时 / 集群不可达。
     */
    TIMEOUT("timeout", "Broker 超时或集群不可达，请检查 Kafka 集群与网络"),

    /**
     * 其它异常。
     */
    ERROR("error", "预检异常，详见日志（不记录凭证）");

    private final String tag;
    private final String disposition;

    MdmPreflightClassification(String tag, String disposition) {
        this.tag = tag;
        this.disposition = disposition;
    }

    /**
     * 指标 result 标签值。
     */
    public String tag() {
        return tag;
    }

    /**
     * 处置建议（日志输出）。
     */
    public String disposition() {
        return disposition;
    }
}
