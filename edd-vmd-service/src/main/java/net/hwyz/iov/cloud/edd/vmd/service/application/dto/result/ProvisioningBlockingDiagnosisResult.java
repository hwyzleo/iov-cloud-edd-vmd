package net.hwyz.iov.cloud.edd.vmd.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MPT 一键清理阻塞——阻塞诊断结果（VMD-DSN-CR-065 §6.2）。
 * <p>
 * 仅用于页面展示，不产生授权令牌；执行时必须重做全部门禁。
 * 不返回证书材料、密钥材料、Wrapped Key 或任何敏感摘要原文。
 *
 * @author hwyz_leo
 * @since 2026-10-22
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProvisioningBlockingDiagnosisResult {

    /** 目标批次 requestId */
    private String requestId;

    /** TBOX 序列号 */
    private String deviceSn;

    /** 业务域 */
    private String businessDomain;

    /** 批次状态 */
    private String status;

    /** 批次最近修改时间（ISO-8601，clear-blocking 回传防过期页面 STALE_VIEW 校验） */
    private LocalDateTime modifyTime;

    /** 滞留时长（秒） */
    private Long staleDurationSeconds;

    /** 原失败原因（非敏感） */
    private String failReason;

    /** 批次证书摘要与当前 ACTIVE 证书是否不一致（“证书已升级”），不含证书材料 */
    private Boolean certificateChanged;

    /** operation 汇总：未开始 / 确定失败 / 成功 / 处理中或未知 */
    private OperationSummary operationSummary;

    /** 关联 PENDING 目录数量 */
    private Integer pendingDirectoryCount;

    /** 是否占用“同设备+业务域最多一个未终结批次”唯一约束 */
    private Boolean uniqueConstraintOccupied;

    /** 是否可一键清理（clearable=true 时仅表示预检通过，执行仍须重做门禁） */
    private Boolean clearable;

    /** 阻断原因列表（不可清理时给运维的明确原因） */
    private List<String> blockingReasons;

    /** 清理后是否允许新 requestId + 当前 ACTIVE 证书重开批次 */
    private Boolean reopenAllowedAfterClear;

    /** 处置建议：CLEARABLE / RECONCILE_FIRST / WAIT_ACK_EXPIRY / NOT_CLEARABLE */
    private String suggestedAction;

    /**
     * operation 汇总
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperationSummary {
        /** 未开始（无 operationId 或 framework 调用前失败） */
        private int notStarted;
        /** 确定失败（FAILED/REJECTED/NOT_COMMITTED） */
        private int confirmedFailed;
        /** 成功（SUCCEEDED） */
        private int succeeded;
        /** 处理中或未知（PROCESSING/UNKNOWN/查询不可用） */
        private int inProgressOrUnknown;
    }
}
