package net.hwyz.iov.cloud.edd.vmd.service.application.event.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

/**
 * 设备业务密钥变更事件（CR-055 §7.4）
 * <p>
 * VMD 作为生产者经 Outbox→Relay 发布到 Kafka；payload 仅含非敏感目录信息，
 * 不含 kmsKeyRef、Wrapped Key 或任何密钥材料。
 *
 * @param deviceSn           设备实例序列号
 * @param businessDomain     受治理业务域代码
 * @param purpose            受治理用途代码
 * @param keyId              framework/KMS 不透明标识
 * @param businessKeyVersion VMD 业务版本
 * @param state              变更动作：ACTIVE/ROTATED/REVOKING/REVOKED/EXPIRED
 * @param occurredAt         事件发生时间
 * @author hwyz_leo
 * @since 2026-10-08
 */
@Getter
@AllArgsConstructor
public class BusinessKeyChangedEvent {

    private final String deviceSn;
    private final String businessDomain;
    private final String purpose;
    private final String keyId;
    private final Long businessKeyVersion;
    private final String state;
    private final Instant occurredAt;
}
