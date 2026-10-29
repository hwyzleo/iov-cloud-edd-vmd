package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.hwyz.iov.cloud.framework.common.domain.DomainObj;

import java.time.LocalDateTime;

/**
 * 统一导入操作明细实体（CR-071 §7.2）
 * <p>
 * 逐项幂等键（uk_operation_item）、阶段/状态、检查点（attempt/outbox/event）、
 * 失败重试与恢复依据。
 *
 * @author hwyz_leo
 * @since 2026-10-29
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class ImportOperationItem implements DomainObj<ImportOperationItem> {

    /** 主键 */
    private Long id;

    /** 操作号 */
    private String operationId;

    /** 逐项幂等键：SHA-256(domain,稳定行键,entity,action,计划事实版本) */
    private byte[] itemKey;

    /** 候选标识（VIN / partCode:sn） */
    private String candidateKey;

    /** 实体类型：VEHICLE_BASIC/PART_INSTANCE/VEHICLE_PART/SOFTWARE_INSTALLATION 等 */
    private String entityType;

    /** 实体ID */
    private Long entityId;

    /** VIN */
    private String vin;

    /** 动作类型（与既有 replay/后置动作枚举复用） */
    private String actionType;

    /** 阶段：PLAN/BASIC/BINDING/SOFTWARE/SECURITY/LIFECYCLE/FORWARD/EVENT */
    private String phase;

    /** 状态：PENDING/RUNNING/QUEUED/SUCCEEDED/NOOP/SKIPPED/FAILED_RETRYABLE/FAILED_FINAL */
    private String status;

    /** 来源系统 */
    private String source;

    /** 来源事件ID */
    private String sourceEventId;

    /** 计划事实内容摘要 */
    private byte[] contentDigest;

    /** 执行前预期业务本体版本 */
    private Long expectedRowVersion;

    /** 执行后业务本体版本 */
    private Long resultRowVersion;

    /** 实际变更字段差异 */
    private String changedFields;

    /** 预冻结事件ID（REPLAY/Outbox） */
    private String eventId;

    /** 关联 vmd_outbox 主键 */
    private Long outboxId;

    /** 旧补发逐项审计引用 */
    private String legacyItemRef;

    /** 尝试次数 */
    private Integer attemptCount;

    /** 跳过原因 */
    private String skipReason;

    /** 错误码 */
    private String errorCode;

    /** 错误信息 */
    private String errorMessage;

    /** 开始时刻 */
    private LocalDateTime startedAt;

    /** 结束时刻 */
    private LocalDateTime finishedAt;

}
