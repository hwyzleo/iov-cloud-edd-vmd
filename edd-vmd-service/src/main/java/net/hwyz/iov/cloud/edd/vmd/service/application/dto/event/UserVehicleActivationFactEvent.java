package net.hwyz.iov.cloud.edd.vmd.service.application.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * IOV-TSP 车辆首次激活候选事实事件（VMD-DSN-CR-077 §4.1，schemaVersion=1）
 * <p>
 * 来源 Topic {@code tsp.user-vehicle-activation-fact}（CONSUMER_ONLY，Key=VIN），
 * 生产端经 Transactional Outbox 投递。
 * <p>
 * 反序列化为「宽松-严格混合」：未知字段忽略（前向兼容），缺必填由应用层校验后隔离，
 * 不做隐式默认值填充。{@code occurredAt} 保持字符串接收，由应用层显式解析——
 * 解析失败属契约错误（{@code QUARANTINED}），而非技术错误（不重试）。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserVehicleActivationFactEvent {

    /**
     * 事件 ID（生产端 newId("UVA")，幂等键 1）
     */
    private String eventId;

    /**
     * 契约版本（固定 1，其它版本隔离）
     */
    private Integer schemaVersion;

    /**
     * 车架号（≤20，Kafka Key）
     */
    private String vin;

    /**
     * 绑定操作 ID（幂等键 2）
     */
    private String bindingOperationId;

    /**
     * 来源事实引用（可空，≤512，不含 PII）
     */
    private String sourceFactRef;

    /**
     * 事实发生绝对时刻（ISO-8601，如 2026-11-22T09:45:01.234Z）
     * <p>参与摘要哈希时按<strong>字符串原文</strong>使用，不做解析再序列化</p>
     */
    private String occurredAt;

    /**
     * 载荷摘要（可空；终审修复 B-4 后生产端已投递）
     * <p>定稿口径 sha256(vin|bindingOperationId|sourceFactRef|occurredAt)，与 eventId 无关</p>
     * <p>提供时即权威值（{@code digest_source=EVENT}），VMD 本地复算仅作完整性校验</p>
     */
    private String payloadDigest;

}
