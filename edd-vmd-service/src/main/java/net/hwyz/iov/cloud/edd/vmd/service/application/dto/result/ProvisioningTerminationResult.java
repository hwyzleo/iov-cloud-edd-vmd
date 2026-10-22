package net.hwyz.iov.cloud.edd.vmd.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 滞留开通批次运维终止结果（VMD-DSN-CR-065 §3.1）。
 *
 * @author hwyz_leo
 * @since 2026-10-22
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProvisioningTerminationResult {

    /** 目标批次 requestId */
    private String requestId;

    /** 终止前批次状态 */
    private String beforeState;

    /** 终止后批次状态（FAILED 或原终态） */
    private String afterState;

    /** 是否发生状态变更（FAILED/EXPIRED 幂等重复执行为 false） */
    private boolean changed;

    /** 是否已清理阻塞（终止成功或批次已是终态释放约束时为 true；门禁失败不返回本结果） */
    private boolean blockingCleared;

    /** 终止后是否允许新 requestId + 当前 ACTIVE 证书重开批次（唯一约束已释放） */
    private boolean reopenAllowed;

    /** 本批次被连带失败化的 PENDING 目录数 */
    private int failedPendingKeyCount;

    /** 阻断原因（未通过门禁时给运维的明确原因；成功时为空） */
    private List<String> blockingReasons;

    /** 操作时间 */
    private LocalDateTime operatedAt;
}
