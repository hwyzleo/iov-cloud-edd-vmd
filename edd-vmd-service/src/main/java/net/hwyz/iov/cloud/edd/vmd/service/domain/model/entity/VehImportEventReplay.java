package net.hwyz.iov.cloud.edd.vmd.service.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.framework.common.domain.BaseDo;

import java.time.LocalDateTime;

/**
 * 车辆导入事件补发主任务审计领域实体
 * <p>
 * VMD-DSN-CR-039: 车辆导入成功事件人工补发（PRODUCE 单事件）
 * VMD-DSN-CR-057: 扩展为按 ImportType 路由的动作补偿主任务
 *
 * @author hwyz_leo
 * @since 2026-07-17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehImportEventReplay extends BaseDo<Long> {

    private Long id;
    private String replayId;
    private Long vehImportDataId;
    private String batchNum;
    /** 导入类型（PRODUCE/TOL/EOL，动作路由依据；原 event_type 更名） */
    private String importType;
    /** 请求动作范围：逗号分隔的动作类型子集，空表示全部适用动作 */
    private String requestedActions;
    private String operatorId;
    private String operatorName;
    private String reason;
    /** 状态：PENDING/RUNNING/QUEUED/SUCCESS/SUCCEEDED_WITH_SKIPS/PARTIAL_FAILED/FAILED */
    private String status;
    private Integer totalCount;
    /** 已入队数（写入Outbox） */
    private Integer queuedCount;
    /** 成功动作数 */
    private Integer successCount;
    /** 跳过动作数 */
    private Integer skipCount;
    private Integer failureCount;
    private String failureDetail;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createTime;
}
