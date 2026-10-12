package net.hwyz.iov.cloud.edd.vmd.service.application.event.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * TBOX V2C 上下行业务密钥件内导入完整成功事件（CR-058 §6.2）。
 * <p>
 * VMD 作为生产者经 Outbox→Relay 发布到 vmd.tbox-v2c-key-provisioned（Kafka Key=deviceSn）；
 * 仅供 IOV-TSP 收口设备注册激活（ACTIVATING→ACTIVE）。payload 仅含非敏感元数据，
 * 不含 VIN、HSM UID、kmsKeyRef、Wrapped Key、设备证书或任何密钥材料。
 *
 * @author hwyz_leo
 * @since 2026-10-12
 */
@Getter
@AllArgsConstructor
public class TboxV2cKeyProvisionedEvent {

    /**
     * 事件唯一标识（写 Outbox 前生成）
     */
    private final String eventId;

    /**
     * TBOX 稳定 requestId
     */
    private final String requestId;

    /**
     * TBOX 序列号（同时为 Kafka Key）
     */
    private final String deviceSn;

    /**
     * 业务域；首期固定 V2C_COMM
     */
    private final String businessDomain;

    /**
     * 完整用途密钥元数据
     */
    private final List<Key> keys;

    /**
     * 聚合结果；仅发布 SUCCEEDED
     */
    private final String result;

    /**
     * 事件发生时间
     */
    private final Instant occurredAt;

    /**
     * 单用途密钥元数据（不含密钥材料）
     */
    @Getter
    @AllArgsConstructor
    public static class Key {
        /**
         * 用途代码
         */
        private final String purpose;

        /**
         * VMD 业务目录 ID
         */
        private final String keyId;

        /**
         * VMD 业务版本
         */
        private final Long businessKeyVersion;
    }
}
