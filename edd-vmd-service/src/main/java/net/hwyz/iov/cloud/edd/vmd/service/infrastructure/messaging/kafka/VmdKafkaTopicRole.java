package net.hwyz.iov.cloud.edd.vmd.service.infrastructure.messaging.kafka;

/**
 * VMD Kafka Topic 角色（所有权）
 * <p>
 * VMD-DSN-CR-051：仅对 PRODUCER_OWNED 的 Topic 承担初始化（查询→缺失创建→既有校验）职责；
 * CONSUMER_ONLY 的 Topic 生命周期归其生产者（IOV-OTA），VMD 只做可访问性探测，不创建。
 *
 * @author hwyz_leo
 */
public enum VmdKafkaTopicRole {

    /**
     * VMD 作为生产者，Topic 缺失时由 VMD 幂等创建，并对既有配置做校验。
     */
    PRODUCER_OWNED,

    /**
     * VMD 仅作为消费者，Topic 由上游生产者管理，VMD 不创建、不改写。
     */
    CONSUMER_ONLY
}
