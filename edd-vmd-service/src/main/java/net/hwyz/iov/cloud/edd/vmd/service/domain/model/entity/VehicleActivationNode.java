package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.hwyz.iov.cloud.framework.common.domain.DomainObj;

import java.time.LocalDateTime;

/**
 * 车辆首次激活节点投影 领域实体（VMD-DSN-CR-077 / D-VMD-077-03 / D-VMD-077-04）
 * <p>
 * per-VIN 单行「首次激活生命周期节点」投影：唯一键 {@code vin}，语义为
 * 「已消费激活事实中 {@code occurred_at} 最小者」，只前不后（乱序纠正），
 * 与激活状态机（已由 V23 删除的 {@code tb_veh_activation}）无继承关系。
 * <p>
 * 本实体为纯终端投影：不写 {@code vmd_outbox}、不发二次事件、不写 {@code tb_veh_lifecycle}，
 * 也不因 VIN 未建档而伪造车辆主档（D-VMD-077-08）。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
public class VehicleActivationNode implements DomainObj<VehicleActivationNode> {

    /**
     * 主键
     */
    private Long id;

    /**
     * 车架号（Kafka Key；唯一键）
     */
    private String vin;

    /**
     * 首次激活绝对时刻（UTC，已消费事实 occurred_at 最小值）
     */
    private LocalDateTime firstActivatedAt;

    /**
     * 决定首次激活的绑定操作 ID
     */
    private String bindingOperationId;

    /**
     * 来源事实引用（不含 PII）
     */
    private String sourceFactRef;

    /**
     * 载荷摘要 sha256(vin|bindingOperationId|sourceFactRef|occurredAt)，与 eventId 无关
     */
    private String payloadDigest;

    /**
     * 决定该投影的事件 ID
     */
    private String eventId;

    /**
     * 事实发生绝对时刻（UTC）
     */
    private LocalDateTime occurredAt;

    /**
     * VMD 首次接收该事实的绝对时刻（UTC）
     */
    private LocalDateTime receivedAt;

    /**
     * 事件契约版本
     */
    private Integer schemaVersion;

    /**
     * 摘要来源：EVENT=事件提供 / LOCAL=本地回算（仅观测，不影响判定）
     */
    private String digestSource;

    /**
     * VIN 是否已在 VMD 建档：0=未建档（仅事实投影，不建车）
     */
    private Integer vinResolved;

    /**
     * 备注（如 CORRECTED）
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

    /**
     * 判断给定事实时刻是否可作为新的「首次激活」（严格早于当前投影）
     *
     * @param candidateOccurredAt 候选事实时刻
     * @return true=需要向前纠正
     */
    public boolean shouldCorrect(LocalDateTime candidateOccurredAt) {
        return candidateOccurredAt != null
                && (firstActivatedAt == null || candidateOccurredAt.isBefore(firstActivatedAt));
    }

}
