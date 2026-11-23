package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.hwyz.iov.cloud.framework.common.domain.DomainObj;

import java.time.LocalDateTime;

/**
 * 车辆首次激活候选事实消费台账 领域实体（VMD-DSN-CR-077 / D-VMD-077-03）
 * <p>
 * 每次消费的证据归档：{@code event_id} 唯一 + {@code (vin, binding_operation_id)} 唯一
 * 构成双重幂等，并发下由唯一键兜底（捕获冲突后重读重判）。
 * <p>
 * 终态集合（R-10 定稿，<strong>不含 CONFLICT</strong>）：
 * {@code RECORDED / DUPLICATE / QUARANTINED / FAILED}；摘要差异仅记
 * {@code DIGEST_MISMATCH} 备注与观测指标，<strong>严禁</strong>升级为冲突终态。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
public class VehicleActivationFactConsume implements DomainObj<VehicleActivationFactConsume> {

    /**
     * 主键
     */
    private Long id;

    /**
     * 事件 ID（幂等键 1）
     */
    private String eventId;

    /**
     * 事件契约版本
     */
    private Integer schemaVersion;

    /**
     * 车架号
     */
    private String vin;

    /**
     * 绑定操作 ID（幂等键 2）
     */
    private String bindingOperationId;

    /**
     * 来源事实引用
     */
    private String sourceFactRef;

    /**
     * 载荷摘要 sha256(vin|bindingOperationId|sourceFactRef|occurredAt)，与 eventId 无关
     */
    private String payloadDigest;

    /**
     * 摘要来源：EVENT / LOCAL（仅观测）
     */
    private String digestSource;

    /**
     * 事实发生绝对时刻（UTC）
     */
    private LocalDateTime occurredAt;

    /**
     * 接收绝对时刻（UTC）
     */
    private LocalDateTime receivedAt;

    /**
     * 状态：RECORDED / DUPLICATE / QUARANTINED / FAILED
     */
    private String status;

    /**
     * 是否作用于投影（0/1）
     */
    private Integer applied;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 错误 / 隔离原因摘要（脱敏、截断）
     */
    private String lastError;

    /**
     * 来源 Topic（重放定位）
     */
    private String topic;

    /**
     * 来源分区
     */
    private Integer partitionNo;

    /**
     * 来源位点
     */
    private Long recordOffset;

    /**
     * 备注（如 CORRECTED / DIGEST_MISMATCH / DIGEST_VERIFY_MISMATCH）
     */
    private String description;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    private LocalDateTime modifyTime;

}
